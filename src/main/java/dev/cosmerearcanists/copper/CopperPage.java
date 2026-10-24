package dev.cosmerearcanists.copper;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
public record CopperPage(UUID id,int kind,String title,String text,String dimension,BlockPos position,int imageBytes) {
    public static final int NOTE=0,WAYPOINT=1,SCREENSHOT=2;
    public CopperPage(UUID id,int kind,String title,String text,String dimension,BlockPos position){this(id,kind,title,text,dimension,position,kind==SCREENSHOT?-1:0);}
    public long dataBytes(){return CopperCapacity.pageBytes(title,text,dimension,imageBytes<0?CopperImages.HARD_BYTES:imageBytes);}
    public CopperPage {if(id==null||kind<0||kind>2||title.length()>80||text.length()>2048||dimension.length()>128||imageBytes< -1||imageBytes>CopperImages.HARD_BYTES||kind!=SCREENSHOT&&imageBytes!=0)throw new IllegalArgumentException("Invalid memory page");position=position.immutable();}
    public CompoundTag save(){var t=new CompoundTag();t.putUUID("id",id);t.putInt("kind",kind);t.putInt("image_bytes",imageBytes);t.putString("title",title);t.putString("text",text);t.putString("dimension",dimension);t.putLong("position",position.asLong());return t;}
    public static CopperPage load(CompoundTag t){return new CopperPage(t.getUUID("id"),t.getInt("kind"),t.getString("title"),t.getString("text"),t.getString("dimension"),BlockPos.of(t.getLong("position")),t.contains("image_bytes")?t.getInt("image_bytes"):t.getInt("kind")==SCREENSHOT?-1:0);}
}
