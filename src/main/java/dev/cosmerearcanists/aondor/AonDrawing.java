package dev.cosmerearcanists.aondor;

import java.util.*;

/** Normalized canvas coordinates, with ordered pen-down strokes inside each glyph. */
public record AonDrawing(List<Glyph> glyphs) {
    public static final int MAX_GLYPHS=17, MAX_STROKES=16, MAX_POINTS=48, MAX_TOTAL_POINTS=3000;
    public record Point(double x,double y) {
        public Point {if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1)throw new IllegalArgumentException("Point outside canvas");}
    }
    public record Glyph(boolean effect,double x,double y,double size,List<List<Point>> strokes) {
        public Glyph(boolean effect,double x,double y,List<List<Point>> strokes){this(effect,x,y,.18,strokes);}
        public Glyph {
            new Point(x,y);
            if(!Double.isFinite(size)||size<.02||size>1)throw new IllegalArgumentException("Invalid Aon drawing size");
            if(strokes.size()>MAX_STROKES)throw new IllegalArgumentException("Too many strokes");
            strokes=strokes.stream().map(stroke->{if(stroke.size()<2||stroke.size()>MAX_POINTS)throw new IllegalArgumentException("Invalid stroke length");return List.copyOf(stroke);}).toList();
        }
    }
    public AonDrawing {
        glyphs=List.copyOf(glyphs);
        if(glyphs.isEmpty()||glyphs.size()>MAX_GLYPHS||glyphs.stream().filter(Glyph::effect).count()!=1)
            throw new IllegalArgumentException("Exactly one main effect Aon is required");
        if(glyphs.stream().flatMap(g->g.strokes().stream()).mapToInt(List::size).sum()>MAX_TOTAL_POINTS)
            throw new IllegalArgumentException("Drawing is too large");
    }
    public Glyph main(){return glyphs.stream().filter(Glyph::effect).findFirst().orElseThrow();}
    /** Screen coordinates: twelve o'clock, then right/down/left. Equal angles use nearer first. */
    public List<Glyph> arguments() {
        var main=main();var args=new ArrayList<>(glyphs.stream().filter(g->!g.effect()).toList());
        args.sort(Comparator.comparingDouble((Glyph g)->angle(main,g)).thenComparingDouble(g->Math.hypot(g.x()-main.x(),g.y()-main.y())));
        return List.copyOf(args);
    }
    public static double angle(Glyph main,Glyph argument) {
        double angle=Math.atan2(argument.x()-main.x(),main.y()-argument.y());
        return angle<0?angle+2*Math.PI:angle;
    }
}
