package dev.cosmerearcanists.aondor;
import java.util.*;
import net.minecraft.nbt.*;
/** Stored raw geometry uses the same strict budgets as network drawings, never executable client IDs. */
public final class AonDrawingNbt {
    public static CompoundTag save(AonDrawing drawing){var root=new CompoundTag();var glyphs=new ListTag();for(var g:drawing.glyphs()){var tag=new CompoundTag();tag.putBoolean("effect",g.effect());tag.putDouble("x",g.x());tag.putDouble("y",g.y());tag.putDouble("size",g.size());tag.putInt("parent",g.parent());tag.putDouble("number",g.number());var strokes=new ListTag();for(var line:g.strokes()){var stroke=new CompoundTag();var points=new ListTag();for(var p:line){var point=new CompoundTag();point.putDouble("x",p.x());point.putDouble("y",p.y());points.add(point);}stroke.put("points",points);strokes.add(stroke);}tag.put("strokes",strokes);glyphs.add(tag);}root.put("glyphs",glyphs);return root;}
    public static AonDrawing load(CompoundTag root){var glyphs=root.getList("glyphs",Tag.TAG_COMPOUND);if(glyphs.size()<1||glyphs.size()>AonDrawing.MAX_GLYPHS)throw new IllegalArgumentException("Saved spell exceeds glyph limits");var result=new ArrayList<AonDrawing.Glyph>();int budget=AonDrawing.MAX_TOTAL_POINTS;
        for(int i=0;i<glyphs.size();i++){var t=glyphs.getCompound(i);var strokes=t.getList("strokes",Tag.TAG_COMPOUND);if(strokes.size()>AonDrawing.MAX_STROKES)throw new IllegalArgumentException("Saved spell exceeds stroke limits");var lines=new ArrayList<List<AonDrawing.Point>>();for(int j=0;j<strokes.size();j++){var points=strokes.getCompound(j).getList("points",Tag.TAG_COMPOUND);if(points.size()<2||points.size()>AonDrawing.MAX_POINTS||(budget-=points.size())<0)throw new IllegalArgumentException("Saved spell exceeds point limits");var line=new ArrayList<AonDrawing.Point>();for(int k=0;k<points.size();k++){var p=points.getCompound(k);line.add(new AonDrawing.Point(p.getDouble("x"),p.getDouble("y")));}lines.add(line);}result.add(new AonDrawing.Glyph(t.getBoolean("effect"),t.getDouble("x"),t.getDouble("y"),t.getDouble("size"),lines,t.getInt("parent"),t.getDouble("number")));}return new AonDrawing(result);
    }
    private AonDrawingNbt(){}
}
