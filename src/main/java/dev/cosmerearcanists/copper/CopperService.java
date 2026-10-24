package dev.cosmerearcanists.copper;

import dev.cosmerearcanists.*;
import dev.cosmerearcanists.feruchemy.*;
import dev.cosmerearcanists.power.*;
import java.io.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import static dev.cosmerearcanists.copper.CopperNetwork.*;

public final class CopperService {
    private static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger(CopperService.class);
    private static final Set<UUID> WARNED_ERASURES=new HashSet<>();
    private static final String KEY="cosmerearcanists_copper_book";
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Map<UUID,Long> UPLOAD_TICKS=new HashMap<>();
    private static final Map<UUID,Integer> MODES=new HashMap<>();
    private static final Map<UUID,long[]> BUDGETS=new HashMap<>();
    private static final Map<UUID,Tracked> TRACKED=new HashMap<>();
    private record Tracked(UUID book,UUID page){}
    private static final class Upload {UUID transfer,id;String title,text;int total;long expires;ByteArrayOutputStream bytes=new ByteArrayOutputStream();}
    private static final class Download {UUID id;byte[] bytes;int offset;}
    public static final class Session {public final ServerPlayer player;public final UUID token=UUID.randomUUID();public final UUID book;final int mode;Upload upload;Download download;Session(ServerPlayer player,UUID book,int mode){this.player=player;this.book=book;this.mode=mode;}}
    public static boolean configured(){return PowerConfig.ENABLED.get()&&FeruchemyConfig.ENABLED.get()&&FeruchemyConfig.COPPER.get()&&ServerConfig.FERUCHEMY.enabled().get()&&ServerConfig.FERUCHEMY.metals().get(Metal.COPPER).get();}
    private static boolean eligible(ServerPlayer p){return configured()&&p.isAlive()&&!p.isSpectator()&&PowerAttributes.strength(p,"f:copper")>0&&PowerData.state(p,"f:copper")!=0;}
    public static UUID identity(ItemStack s){var t=s.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();return t.hasUUID(KEY)?t.getUUID(KEY):ZERO;}
    private static List<Metalminds.Access> pieces(ServerPlayer p){var result=new ArrayList<Metalminds.Access>();var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());for(var a:Metalminds.touching(p))if(seen.add(a.stack())&&Metalminds.usableBy(a.stack(),p,"copper")&&(PowerData.state(p,"f:copper")>0||Metalminds.writable(p,a.stack(),"copper")))result.add(a);return result;}
    private static Metalminds.Access find(ServerPlayer p,UUID book){return pieces(p).stream().filter(a->Metalminds.bound(a.stack())&&identity(a.stack()).equals(book)).findFirst().orElse(null);}
    private static UUID prepare(ServerPlayer p,Metalminds.Access a){
        UUID id=identity(a.stack());var data=CopperBooks.get(p.server);if(!id.equals(ZERO)){if(data.books.containsKey(id)){resolveLegacyImageSizes(p,id);return id;}
            // A copied item may still carry an erased identity. It can never resurrect the old notebook.
            Metalminds.wipe(a.stack());a.changed().run();if(PowerData.state(p,"f:copper")>=0)return ZERO;
        }
        if(PowerData.state(p,"f:copper")>=0||data.books.size()>=FeruchemyConfig.COPPER_BOOKS.get())return ZERO;
        if(!Metalminds.bound(a.stack())&&Metalminds.transferForStorage(p,a.stack(),"copper",1)<1-1e-9)return ZERO;
        id=UUID.randomUUID();var root=a.stack().getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();root.putUUID(KEY,id);a.stack().set(DataComponents.CUSTOM_DATA,CustomData.of(root));a.changed().run();data.books.put(id,new LinkedHashMap<>());data.setDirty();return id;
    }
    private static void resolveLegacyImageSizes(ServerPlayer p,UUID book){
        var data=CopperBooks.get(p.server);var pages=data.books.get(book);
        for(var page:List.copyOf(pages.values()))if(page.kind()==CopperPage.SCREENSHOT&&page.imageBytes()<0){
            int bytes=CopperImages.HARD_BYTES;
            try{long size=java.nio.file.Files.size(CopperImages.directory(p.server).resolve(page.id()+".png"));if(size>=8&&size<=CopperImages.HARD_BYTES)bytes=(int)size;}catch(IOException ignored){}
            // Missing/unreadable legacy files reserve the maximum; access never discards the old memory.
            pages.put(page.id(),new CopperPage(page.id(),page.kind(),page.title(),page.text(),page.dimension(),page.position(),bytes));data.setDirty();
        }
    }
    /** Called only by authoritative server-side wipe/empty transitions; also works while copper is disabled. */
    public static boolean eraseItem(ItemStack stack){
        UUID book=identity(stack);if(book.equals(ZERO))return false;
        var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null||!server.isSameThread())return false;
        eraseBook(server,book);
        var root=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();root.remove(KEY);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(root));return true;
    }
    public static void eraseBook(net.minecraft.server.MinecraftServer server,UUID book){
        var data=CopperBooks.get(server);var pages=data.books.remove(book);
        if(pages!=null){var images=new LinkedHashSet<UUID>();for(var page:pages.values())if(page.kind()==CopperPage.SCREENSHOT)images.add(page.id());images.addAll(data.erasedImages);data.erasedImages.clear();data.erasedImages.addAll(images);data.setDirty();}
        for(var session:List.copyOf(SESSIONS.values()))if(session.book.equals(book))close(session.player,"This coppermind has been emptied; its memories were erased.");
        for(var entry:List.copyOf(TRACKED.entrySet()))if(entry.getValue().book.equals(book)){
            var session=SESSIONS.get(entry.getKey());var player=session!=null?session.player:server.getPlayerList().getPlayer(entry.getKey());if(player!=null)clearWaypoint(player);else TRACKED.remove(entry.getKey());
        }
        // Persist the loss of access and cleanup queue before deleting files; failed filesystem operations remain retryable after restart.
        server.overworld().getDataStorage().save();retryErasedImages(server,64);
    }
    public static void retryErasedImages(net.minecraft.server.MinecraftServer server,int limit){
        var data=CopperBooks.get(server);boolean changed=false;int count=0;
        for(var id:List.copyOf(data.erasedImages)){if(count++>=limit)break;try{CopperImages.delete(server,id);data.erasedImages.remove(id);WARNED_ERASURES.remove(id);changed=true;}catch(IOException e){data.erasedImages.remove(id);data.erasedImages.add(id);data.setDirty();if(WARNED_ERASURES.add(id))LOGGER.warn("Could not delete erased coppermind image {}; cleanup will retry",id,e);}}
        if(changed){data.setDirty();server.overworld().getDataStorage().save();}
    }
    @SubscribeEvent public void erasedImageCleanup(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){if(e.getServer().getTickCount()%20==0)retryErasedImages(e.getServer(),64);}
    public static Session session(ServerPlayer p){return SESSIONS.get(p.getUUID());}
    public static Session open(ServerPlayer p,boolean next){
        if(!eligible(p)){close(p,"");return null;}var list=pieces(p);var previous=session(p);int start=0;
        if(next&&previous!=null)for(int i=0;i<list.size();i++)if(identity(list.get(i).stack()).equals(previous.book)){start=i+1;break;}
        for(int i=0;i<list.size();i++){var access=list.get((start+i)%list.size());UUID book=prepare(p,access);if(book.equals(ZERO))continue;var s=new Session(p,book,Integer.signum(PowerData.state(p,"f:copper")));SESSIONS.put(p.getUUID(),s);snapshot(p,s,"");return s;}
        close(p,"Hold or wear one copper metalmind; use storing mode to start a notebook.");return null;
    }
    private static void snapshot(ServerPlayer p,Session s,String message){var a=find(p,s.book);if(a==null){close(p,"");return;}String label=a.stack().getHoverName().getString();if(label.length()>128)label=label.substring(0,128);var pages=CopperBooks.get(p.server).books.get(s.book);PacketDistributor.sendToPlayer(p,new Snapshot(true,s.token,s.book,label,PowerData.state(p,"f:copper")<0,new ArrayList<>(pages.values()),message,usedBytes(p,s),capacityBytes(p,a.stack()),CopperCapacity.bytes(Metalminds.capacity(a.stack()),bytesPerCharge(p))));}
    private static void close(ServerPlayer p,String message){if(SESSIONS.remove(p.getUUID())!=null||!message.isEmpty())PacketDistributor.sendToPlayer(p,new Snapshot(false,ZERO,ZERO,"",false,List.of(),message));clearWaypoint(p);}
    public static void clear(ServerPlayer p){close(p,"");MODES.remove(p.getUUID());UPLOAD_TICKS.remove(p.getUUID());BUDGETS.remove(p.getUUID());}
    private static void clearWaypoint(ServerPlayer p){if(TRACKED.remove(p.getUUID())!=null)PacketDistributor.sendToPlayer(p,new Waypoint(false,"","",BlockPos.ZERO));}
    private static boolean allowed(ServerPlayer p,Session s){return s!=null&&eligible(p)&&s.mode==Integer.signum(PowerData.state(p,"f:copper"))&&find(p,s.book)!=null&&CopperBooks.get(p.server).books.containsKey(s.book);}
    private static boolean budget(ServerPlayer p){long now=p.server.overworld().getGameTime();var b=BUDGETS.computeIfAbsent(p.getUUID(),k->new long[]{now,0});if(now-b[0]>=20||now<b[0]){b[0]=now;b[1]=0;}return ++b[1]<=80;}
    public static void request(ServerPlayer p,Request r){
        if(!budget(p))return;if(r.action()==OPEN){open(p,false);return;}
        var s=session(p);if(!allowed(p,s)||!s.token.equals(r.session()))return;
        if(r.action()==NEXT){open(p,true);return;}
        var data=CopperBooks.get(p.server);var pages=data.books.get(s.book);var page=pages.get(r.entry());
        try{
            if(r.action()==IMAGE){if(!FeruchemyConfig.COPPER_SCREENSHOTS.get()||page==null||page.kind()!=CopperPage.SCREENSHOT)return;if(SESSIONS.values().stream().filter(x->x.download!=null).count()>=8)throw new IOException("Image transfers are busy; try again");var d=new Download();d.id=page.id();d.bytes=CopperImages.read(p.server,page.id());s.download=d;return;}
            if(r.action()==TRACK){if(PowerData.state(p,"f:copper")<=0)return;if(page==null||page.kind()!=CopperPage.WAYPOINT){clearWaypoint(p);return;}var oldTrack=TRACKED.get(p.getUUID());if(oldTrack!=null&&oldTrack.book.equals(s.book)&&oldTrack.page.equals(page.id())){clearWaypoint(p);return;}TRACKED.put(p.getUUID(),new Tracked(s.book,page.id()));PacketDistributor.sendToPlayer(p,new Waypoint(true,page.title(),page.dimension(),page.position()));return;}
            if(PowerData.state(p,"f:copper")>=0)return;
            switch(r.action()){
                case SAVE -> {var id=page==null?UUID.randomUUID():page.id();var update=new CopperPage(id,page==null?CopperPage.NOTE:page.kind(),title(r.title()),text(r.text()),page==null?"":page.dimension(),page==null?BlockPos.ZERO:page.position(),page==null?0:page.imageBytes());put(p,s,update);}
                case LOCATION -> put(p,s,new CopperPage(UUID.randomUUID(),CopperPage.WAYPOINT,title(r.title()),text(r.text()),p.level().dimension().location().toString(),p.blockPosition()));
                case DELETE -> {if(page!=null){if(page.kind()==CopperPage.SCREENSHOT)CopperImages.delete(p.server,page.id());pages.remove(page.id());data.setDirty();var track=TRACKED.get(p.getUUID());if(track!=null&&track.page.equals(page.id()))clearWaypoint(p);snapshot(p,s,"");}}
                case BEGIN -> {long now=p.server.overworld().getGameTime();if(now-UPLOAD_TICKS.getOrDefault(p.getUUID(),now-100)<100)throw new IOException("Wait a few seconds before capturing another screenshot");if(!FeruchemyConfig.COPPER_SCREENSHOTS.get()||r.number()<8||r.number()>FeruchemyConfig.COPPER_IMAGE_BYTES.get()||pages.size()>=FeruchemyConfig.COPPER_PAGES.get())throw new IOException("Screenshot or notebook limit reached");if(SESSIONS.values().stream().filter(x->x.upload!=null).count()>=8)throw new IOException("Screenshot transfers are busy; try again");var u=new Upload();u.transfer=r.entry();u.id=UUID.randomUUID();u.title=title(r.title());u.text=text(r.text());u.total=r.number();u.expires=p.server.overworld().getGameTime()+200;s.upload=u;UPLOAD_TICKS.put(p.getUUID(),now);}
                case CHUNK -> {var u=s.upload;if(u==null||p.server.overworld().getGameTime()>u.expires||!u.transfer.equals(r.entry())||r.number()!=u.bytes.size()||r.bytes().length==0||u.bytes.size()+r.bytes().length>u.total){s.upload=null;return;}u.bytes.write(r.bytes());}
                case FINISH -> {var u=s.upload;s.upload=null;if(u==null||p.server.overworld().getGameTime()>u.expires||!u.transfer.equals(r.entry())||u.bytes.size()!=u.total)return;byte[] png=CopperImages.normalize(u.bytes.toByteArray());var image=new CopperPage(u.id,CopperPage.SCREENSHOT,u.title,u.text,p.level().dimension().location().toString(),p.blockPosition(),png.length);validatePage(p,s,image);CopperImages.write(p.server,u.id,png);try{put(p,s,image);}catch(IOException e){data.erasedImages.add(u.id);data.setDirty();retryErasedImages(p.server,64);throw e;}}
                default -> {}
            }
        }catch(IOException|IllegalArgumentException e){s.upload=null;s.download=null;snapshot(p,s,e instanceof java.nio.file.FileSystemException?"Server could not access screenshot storage.":safeMessage(e.getMessage()));}
    }
    private static String safeMessage(String s){return s==null?"Memory operation failed.":s.substring(0,Math.min(200,s.length()));}
    private static String title(String value){value=value.replace('\0',' ').replace('\n',' ').replace('\r',' ').replace('\t',' ').strip();return value.isEmpty()?"Untitled memory":value;}
    private static String text(String value){return value.replace('\0',' ');}
    private static int bytesPerCharge(ServerPlayer p){return (int)Math.min(Integer.MAX_VALUE,Math.floor(FeruchemyConfig.COPPER_BYTES_PER_CHARGE.get()*(1+dev.cosmerearcanists.hemalurgy.ImplantService.memoryBonus(p))));}
    public static long capacityBytes(ServerPlayer p,ItemStack stack){return CopperCapacity.bytes(Math.min(Metalminds.charge(stack),Metalminds.capacity(stack)),bytesPerCharge(p));}
    public static long capacityBytes(ItemStack stack){return CopperCapacity.bytes(Math.min(Metalminds.charge(stack),Metalminds.capacity(stack)),FeruchemyConfig.COPPER_BYTES_PER_CHARGE.get());}
    public static long usedBytes(ServerPlayer p,Session s){return CopperBooks.get(p.server).books.get(s.book).values().stream().mapToLong(CopperPage::dataBytes).sum();}
    private static long proposedBytes(ServerPlayer p,Session s,CopperPage page){var old=CopperBooks.get(p.server).books.get(s.book).get(page.id());return usedBytes(p,s)-(old==null?0:old.dataBytes())+page.dataBytes();}
    private static void validatePage(ServerPlayer p,Session s,CopperPage page)throws IOException{
        var pages=CopperBooks.get(p.server).books.get(s.book);
        if(!pages.containsKey(page.id())&&pages.size()>=FeruchemyConfig.COPPER_PAGES.get())throw new IOException("This coppermind is full");
        int length=pages.values().stream().filter(row->!row.id().equals(page.id())).mapToInt(row->row.text().length()).sum()+page.text().length();
        if(length>FeruchemyConfig.COPPER_TEXT.get())throw new IOException("Notebook text limit reached");
        var access=find(p,s.book);if(access==null||!Metalminds.writable(p,access.stack(),"copper"))throw new IOException("This coppermind is no longer writable");
        long proposed=proposedBytes(p,s,page);
        // Legacy/partially drained notebooks remain readable and may shrink, but cannot grow past physical capacity.
        if(proposed>usedBytes(p,s)&&proposed>CopperCapacity.bytes(Metalminds.capacity(access.stack()),bytesPerCharge(p)))throw new IOException("Not enough copper capacity; use a larger metalmind or remove memories.");
    }
    private static void put(ServerPlayer p,Session s,CopperPage page)throws IOException{
        validatePage(p,s,page);var access=find(p,s.book);long proposed=proposedBytes(p,s,page);
        if(proposed>usedBytes(p,s)&&proposed>capacityBytes(p,access.stack())){
            double target=CopperCapacity.requiredCharge(proposed,bytesPerCharge(p));
            double moved=Metalminds.transferForStorage(p,access.stack(),"copper",Math.nextUp(target-Metalminds.charge(access.stack())));
            if(moved>0)access.changed().run();
            if(capacityBytes(p,access.stack())<proposed)throw new IOException("Could not store enough copper charge for this memory.");
        }
        var data=CopperBooks.get(p.server);data.books.get(s.book).put(page.id(),page);data.setDirty();snapshot(p,s,"");
    }
    public static void tick(ServerPlayer p){
        int mode=eligible(p)?Integer.signum(PowerData.state(p,"f:copper")):0;int previous=MODES.getOrDefault(p.getUUID(),0);MODES.put(p.getUUID(),mode);
        if(mode!=previous){if(mode!=0)open(p,false);else close(p,"");}
        var s=session(p);if(s!=null&&!allowed(p,s)){close(p,"");s=null;}
        if(s!=null){if(s.upload!=null&&p.server.overworld().getGameTime()>s.upload.expires)s.upload=null;var d=s.download;if(d!=null){var page=CopperBooks.get(p.server).books.get(s.book).get(d.id);if(!FeruchemyConfig.COPPER_SCREENSHOTS.get()||page==null){s.download=null;}else{int end=Math.min(d.bytes.length,d.offset+CopperImages.CHUNK);PacketDistributor.sendToPlayer(p,new Image(s.token,d.id,d.bytes.length,d.offset,Arrays.copyOfRange(d.bytes,d.offset,end)));d.offset=end;if(end==d.bytes.length)s.download=null;}}}
        var track=TRACKED.get(p.getUUID());if(track!=null&&(mode<=0||find(p,track.book)==null||!CopperBooks.get(p.server).books.get(track.book).containsKey(track.page)))clearWaypoint(p);
    }
    public static double strength(ServerPlayer p){return eligible(p)&&session(p)!=null?.1*PowerAttributes.strength(p,"f:copper"):0;}
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public void stopped(ServerStoppedEvent e){SESSIONS.clear();WARNED_ERASURES.clear();UPLOAD_TICKS.clear();MODES.clear();BUDGETS.clear();TRACKED.clear();}
}
