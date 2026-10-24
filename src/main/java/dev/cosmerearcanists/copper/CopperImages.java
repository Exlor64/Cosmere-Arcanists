package dev.cosmerearcanists.copper;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import dev.cosmerearcanists.feruchemy.FeruchemyConfig;
/** Bounded PNG decoding and server-world persistence; all filenames come from server-generated UUIDs. */
public final class CopperImages {
    public static final int WIDTH=512,HEIGHT=288,CHUNK=16384,HARD_BYTES=786432;
    public static void checkHeader(byte[] bytes)throws IOException{if(bytes.length<33||bytes[0]!=(byte)137||bytes[1]!=80||bytes[2]!=78||bytes[3]!=71||bytes[4]!=13||bytes[5]!=10||bytes[6]!=26||bytes[7]!=10||bytes[12]!=73||bytes[13]!=72||bytes[14]!=68||bytes[15]!=82)throw new IOException("Invalid PNG header");var header=java.nio.ByteBuffer.wrap(bytes);int width=header.getInt(16),height=header.getInt(20);if(width<1||height<1||width>WIDTH||height>HEIGHT)throw new IOException("Screenshot dimensions exceed512x288");}
    public static byte[] normalize(byte[] bytes)throws IOException{
        if(bytes.length<8||bytes.length>FeruchemyConfig.COPPER_IMAGE_BYTES.get())throw new IOException("Screenshot exceeds the image limit");
        checkHeader(bytes);
        try(var input=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Invalid PNG");var reader=readers.next();try{reader.setInput(input,true,true);if(!reader.getFormatName().equalsIgnoreCase("png"))throw new IOException("Only PNG screenshots are accepted");int w=reader.getWidth(0),h=reader.getHeight(0);if(w<1||h<1||w>WIDTH||h>HEIGHT)throw new IOException("Screenshot dimensions exceed512x288");var decoded=reader.read(0);var clean=new java.awt.image.BufferedImage(w,h,java.awt.image.BufferedImage.TYPE_INT_RGB);var graphics=clean.createGraphics();try{graphics.drawImage(decoded,0,0,null);}finally{graphics.dispose();}var output=new ByteArrayOutputStream();ImageIO.write(clean,"png",output);byte[] png=output.toByteArray();if(png.length>FeruchemyConfig.COPPER_IMAGE_BYTES.get())throw new IOException("Encoded screenshot exceeds the limit");return png;}finally{reader.dispose();}}
    }
    public static Path directory(MinecraftServer server){return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("cosmerearcanists_copper_images");}
    public static void write(MinecraftServer server,UUID id,byte[] png)throws IOException{
        Path dir=directory(server);Files.createDirectories(dir);long total=0;int files=0;try(var entries=Files.newDirectoryStream(dir,"*.png")){for(var p:entries){if(++files>=FeruchemyConfig.COPPER_IMAGES.get())throw new IOException("Server screenshot count limit reached");total+=Files.size(p);if(total+png.length>FeruchemyConfig.COPPER_DISK_MB.get()*1024L*1024)throw new IOException("Server screenshot storage limit reached");}}
        if(total+png.length>FeruchemyConfig.COPPER_DISK_MB.get()*1024L*1024)throw new IOException("Server screenshot storage limit reached");
        Path target=dir.resolve(id+".png"),temp=dir.resolve(id+".tmp");try{Files.write(temp,png,StandardOpenOption.CREATE_NEW);try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target);}}finally{Files.deleteIfExists(temp);}
    }
    public static byte[] read(MinecraftServer server,UUID id)throws IOException{Path p=directory(server).resolve(id+".png");long size=Files.size(p);if(size<8||size>HARD_BYTES)throw new IOException("Screenshot unavailable");return Files.readAllBytes(p);}
    public static void delete(MinecraftServer server,UUID id)throws IOException{Files.deleteIfExists(directory(server).resolve(id+".png"));}
}
