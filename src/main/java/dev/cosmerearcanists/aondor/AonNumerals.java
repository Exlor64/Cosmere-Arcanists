package dev.cosmerearcanists.aondor;

import java.util.*;
import static dev.cosmerearcanists.aondor.AonDrawing.*;

/** Numeric literal is entirely ink-derived; the transmitted numeric field is never trusted. */
public final class AonNumerals {
    public record Reading(int value,double error,boolean precise){}
    private static final List<Point> DIAMOND=List.of(new Point(.5,.06),new Point(.94,.5),new Point(.5,.94),new Point(.06,.5),new Point(.5,.06));
    private static final AonRecognition.Template FRAME=new AonRecognition.Template("internal:diamond",false,List.of(DIAMOND));
    private record Ring(int index,double x,double y,double radius){}
    public static List<List<Point>> strokes(int value){
        if(value<0||value>10)throw new IllegalArgumentException("Number literals are 0–10");var out=new ArrayList<List<Point>>();out.add(DIAMOND);out.add(circle(.5,.5,.065));
        for(int i=0;i<value;i++){double angle=-Math.PI/2+i*2*Math.PI/10;out.add(circle(.5+.23*Math.cos(angle),.5+.23*Math.sin(angle),.045));}return List.copyOf(out);
    }
    private static List<Point> circle(double x,double y,double radius){var out=new ArrayList<Point>();for(int i=0;i<=24;i++){double angle=-Math.PI/2+i*2*Math.PI/24;out.add(new Point(x+radius*Math.cos(angle),y+radius*Math.sin(angle)));}return List.copyOf(out);}
    public static Reading read(Glyph glyph,AonRecognition.Tolerance tolerance){
        if(glyph.strokes().size()<2)return null;var bounds=AonLayout.bounds(glyph);double extent=bounds.diameter();if(extent<tolerance.minimumExtent())return null;
        var rings=new ArrayList<Ring>();var frame=new ArrayList<List<Point>>();var frameIndices=new ArrayList<Integer>();
        for(int i=0;i<glyph.strokes().size();i++){
            var points=glyph.strokes().get(i);double minX=1,minY=1,maxX=0,maxY=0;for(var p:points){minX=Math.min(minX,p.x());minY=Math.min(minY,p.y());maxX=Math.max(maxX,p.x());maxY=Math.max(maxY,p.y());}
            double width=maxX-minX,height=maxY-minY,diameter=Math.max(width,height);
            if(diameter>=extent*.025&&diameter<=extent*.24&&Math.min(width,height)/diameter>=.6&&distance(points.get(0),points.get(points.size()-1))<=diameter*.35){
                double x=(minX+maxX)/2,y=(minY+maxY)/2,radius=(width+height)/4;var samples=AonRecognition.resample(points,32);double deviation=0,maxDeviation=0;
                for(var p:samples){double difference=Math.abs(Math.hypot(p.x()-x,p.y()-y)-radius)/radius;deviation+=difference;maxDeviation=Math.max(maxDeviation,difference);}
                if(deviation/samples.size()>.22||maxDeviation>.5)return null;
                rings.add(new Ring(i,x,y,radius));
            }else{frame.add(points);frameIndices.add(i);}
        }
        if(frame.isEmpty()||frame.size()>5||rings.isEmpty()||rings.size()>11||!closedFrame(frame,extent*.1))return null;
        double error=AonRecognition.shapeError(frame,FRAME,tolerance.minimumExtent());double orderedError=AonRecognition.error(frame,FRAME,tolerance);if(error>.065&&orderedError>tolerance.maximumError())return null;
        var frameGlyph=new Glyph(false,.5,.5,frame);var b=AonLayout.bounds(frameGlyph);double size=b.diameter();Ring center=null;
        for(var ring:rings)if(Math.hypot(ring.x()-b.x(),ring.y()-b.y())<=size*.075){if(center!=null)return null;center=ring;}
        if(center==null)return null;
        for(var ring:rings){if(ring==center)continue;double radius=Math.hypot(ring.x()-center.x(),ring.y()-center.y());if(radius<size*.13||radius>size*.42||Math.abs(ring.x()-b.x())+Math.abs(ring.y()-b.y())+ring.radius()*1.42>size*.61)return null;}
        for(int i=0;i<rings.size();i++)for(int j=0;j<i;j++)if(Math.hypot(rings.get(i).x()-rings.get(j).x(),rings.get(i).y()-rings.get(j).y())<(rings.get(i).radius()+rings.get(j).radius())*.85)return null;
        boolean precise=frame.size()==1&&frameIndices.get(0)==0&&center.index()==1&&orderedError<=tolerance.maximumError();
        for(var ring:rings)if(area(glyph.strokes().get(ring.index()))<=0)precise=false;
        return new Reading(rings.size()-1,precise?Math.min(error,orderedError):error,precise);
    }
    private static boolean closedFrame(List<List<Point>> strokes,double tolerance){
        var ends=new ArrayList<Point>();for(var stroke:strokes){ends.add(stroke.get(0));ends.add(stroke.get(stroke.size()-1));}
        for(int i=0;i<ends.size();i++){boolean paired=false;for(int j=0;j<ends.size();j++)if(i!=j&&distance(ends.get(i),ends.get(j))<=tolerance){paired=true;break;}if(!paired)return false;}return true;
    }
    private static double area(List<Point> points){double area=0;for(int i=1;i<points.size();i++)area+=points.get(i-1).x()*points.get(i).y()-points.get(i).x()*points.get(i-1).y();return area;}
    private static double distance(Point a,Point b){return Math.hypot(a.x()-b.x(),a.y()-b.y());}
    private AonNumerals(){}
}
