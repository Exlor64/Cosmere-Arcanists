package dev.cosmerearcanists.hemalurgy;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.*;
public final class HemalurgyMenu extends AbstractContainerMenu {
    public static final DeferredRegister<MenuType<?>> REGISTER=DeferredRegister.create(Registries.MENU,"cosmerearcanists");
    public static final DeferredHolder<MenuType<?>,MenuType<HemalurgyMenu>> TYPE=REGISTER.register("hemalurgy",()->new MenuType<>(HemalurgyMenu::new,FeatureFlags.DEFAULT_FLAGS));
    private final Inventory inventory;private final SimpleContainer body=new SimpleContainer(13);
    public final ContainerData status=new SimpleContainerData(1);
    public HemalurgyMenu(int id,Inventory inventory){super(TYPE.get(),id);this.inventory=inventory;for(int i=0;i<13;i++)addSlot(new Slot(body,i,-10000,-10000){@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}});for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,139+col*18,34+row*18));for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,139+col*18,96));addDataSlots(status);refresh();}
    public static void open(ServerPlayer p){if(!p.isAlive()||p.isSpectator()||!ImplantService.screenAllowed(p)){p.displayClientMessage(Component.literal("Visit Scadrial or acquire an active stolen Scadrial Connection to use Hemalurgy."),true);return;}if(p.containerMenu!=p.inventoryMenu)return;p.openMenu(new SimpleMenuProvider((id,inv,player)->new HemalurgyMenu(id,inv),Component.literal("Hemalurgy")));}
    public void refresh(){if(inventory.player instanceof ServerPlayer p){for(int i=0;i<13;i++)body.setItem(i,ItemStack.EMPTY);for(var i:ImplantService.implants(p))body.setItem(i.point(),ImplantService.display(i));status.set(0,(int)Math.max(-32768,Math.min(32767,Math.round(dev.cosmerearcanists.power.SpiritualIntegrity.get(p)*100))));}}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return !(p instanceof ServerPlayer server)||p.isAlive()&&!p.isSpectator()&&ImplantService.screenAllowed(server);}
    @Override public void clicked(int slot,int button,ClickType type,Player player){if(slot>=0&&slot<13)return;super.clicked(slot,button,type,player);}
    @Override public ItemStack quickMoveStack(Player player,int index){if(index<13||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var item=slot.getItem();var before=item.copy();if(!moveItemStackTo(item,index<40?40:13,index<40?49:40,false))return ItemStack.EMPTY;if(item.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,item);return before;}
}
