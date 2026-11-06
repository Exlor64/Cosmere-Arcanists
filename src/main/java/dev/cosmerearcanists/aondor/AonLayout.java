package dev.cosmerearcanists.aondor;
import java.util.*;
import static dev.cosmerearcanists.aondor.AonDrawing.*;

/** Shared canvas geometry. Parents precede children; explicit fallback ownership resolves ambiguity. */
public final class AonLayout {
    public record Bounds(double x,double y,double diameter,double minX,double minY,double maxX,double maxY){
        public boolean contains(Point p,double margin){return p.x()>=minX-margin&&p.x()<=maxX+margin&&p.y()>=minY-margin&&p.y()<=maxY+margin;}
    }
    public static Bounds bounds(Glyph glyph){
        if(glyph.strokes().isEmpty())return new Bounds(glyph.x(),glyph.y(),glyph.size(),glyph.x(),glyph.y(),glyph.x(),glyph.y());
        double x=1,y=1,X=0,Y=0;for(var stroke:glyph.strokes())for(var p:stroke){x=Math.min(x,p.x());y=Math.min(y,p.y());X=Math.max(X,p.x());Y=Math.max(Y,p.y());}
        return new Bounds((x+X)/2,(y+Y)/2,Math.max(.02,Math.max(X-x,Y-y)),x,y,X,Y);
    }
    /** -1 means outside all attachment bands; -2 means ambiguous. */
    public static int parentAt(List<Glyph> glyphs,int before,Point point){
        int best=-1;double first=Double.POSITIVE_INFINITY,second=Double.POSITIVE_INFINITY;
        for(int i=0;i<before;i++){var g=glyphs.get(i);if(g.strokes().isEmpty())continue;var b=bounds(g);double distance=Math.hypot(point.x()-b.x(),point.y()-b.y());
            if(b.contains(point,b.diameter()*.12)||distance>b.diameter()*2.5)continue;
            double score=distance/b.diameter();if(score<first){second=first;first=score;best=i;}else second=Math.min(second,score);
        }
        return second-first<.18?-2:best;
    }
    public static int[] parents(AonDrawing drawing){
        var glyphs=drawing.glyphs();int[] out=new int[glyphs.size()];Arrays.fill(out,-1);
        for(int i=0;i<glyphs.size();i++){var g=glyphs.get(i);if(g.effect())continue;
            if(g.parent()>=0){if(g.parent()>=i)throw new IllegalArgumentException("Parents must be drawn before their arguments");out[i]=g.parent();}
            else {var b=bounds(g);out[i]=parentAt(glyphs,i,new Point(b.x(),b.y()));}
            if(out[i]<0)throw new IllegalArgumentException(out[i]==-2?"Ambiguous argument parent; select its parent and use Draw Argument.":"Argument is outside its parent's attachment band. Move closer or use Draw Argument.");
        }return out;
    }
    public static List<Integer> children(AonDrawing drawing,int[] parents,int index){
        var b=bounds(drawing.glyphs().get(index));var out=new ArrayList<Integer>();for(int i=0;i<parents.length;i++)if(parents[i]==index)out.add(i);
        out.sort(Comparator.comparingDouble((Integer i)->{var a=bounds(drawing.glyphs().get(i));double angle=Math.atan2(a.x()-b.x(),b.y()-a.y());return angle<0?angle+Math.PI*2:angle;}).thenComparingDouble(i->{var a=bounds(drawing.glyphs().get(i));return Math.hypot(a.x()-b.x(),a.y()-b.y());}));return List.copyOf(out);
    }
    private AonLayout(){}
}
