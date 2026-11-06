package dev.cosmerearcanists.aondor;

import java.util.*;
import static dev.cosmerearcanists.aondor.AonDrawing.*;

/** Size/translation tolerant; stroke order, direction, handedness and aspect remain meaningful. */
public final class AonRecognition {
    public record Template(String id,String name,boolean effect,List<List<Point>> strokes) {
        public Template(String id,boolean effect,List<List<Point>> strokes){this(id,id,effect,strokes);}
        public Template {if(id==null||id.isBlank()||id.length()>128||name==null||name.isBlank()||name.length()>128||strokes.isEmpty())throw new IllegalArgumentException("Invalid Aon template");strokes=new Glyph(effect,.5,.5,strokes).strokes();}
    }
    public record Tolerance(double maximumError,double ambiguityGap,double minimumExtent,
                            double pathWeight,double endpointWeight,double cornerWeight,double lengthWeight) {
        public Tolerance {
            for(double value:new double[]{maximumError,ambiguityGap,minimumExtent,pathWeight,endpointWeight,cornerWeight,lengthWeight})
                if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid recognition tolerance");
            if(maximumError<=0||minimumExtent<=0||pathWeight<=0)throw new IllegalArgumentException("Recognition needs positive shape tolerances");
        }
        public static Tolerance defaults(double error,double gap){return new Tolerance(error,gap,.01,.55,.25,.1,.1);}
    }
    public record Match(Template template,double error,boolean ambiguous) {}
    public record Assessment(Template template,double error,boolean ambiguous,boolean precise) {
        public double potency(double fallbackPotency){return template==null?0:precise?1:fallbackPotency;}
    }
    /** Combined ink geometry: stroke sequence, direction and pen lifts do not affect this score. */
    public static double shapeError(List<List<Point>> strokes,Template template,double minimumExtent){
        var a=normalized(strokes,minimumExtent);var b=normalized(template.strokes(),.000001);
        if(a.isEmpty()||b.isEmpty())return Double.POSITIVE_INFINITY;
        return Math.max(inkDistance(a,b),inkDistance(b,a));
    }
    private static double inkDistance(List<List<Point>> source,List<List<Point>> target){
        double sum=0,worst=0,total=0;
        for(var stroke:source)for(int i=1;i<stroke.size();i++){
            var a=stroke.get(i-1);var b=stroke.get(i);double weight=distance(a,b);
            if(weight<1e-12)continue;
            for(int j=0;j<8;j++){
                double t=(j+.5)/8;var point=new Point(a.x()+(b.x()-a.x())*t,a.y()+(b.y()-a.y())*t);
                double nearest=Double.POSITIVE_INFINITY;
                for(var line:target)for(int k=1;k<line.size();k++)nearest=Math.min(nearest,segmentDistance(point,line.get(k-1),line.get(k)));
                sum+=nearest*weight;total+=weight;worst=Math.max(worst,nearest);
            }
        }
        return total==0?Double.POSITIVE_INFINITY:.65*sum/total+.35*worst;
    }
    private static double segmentDistance(Point p,Point a,Point b){
        double dx=b.x()-a.x(),dy=b.y()-a.y(),length=dx*dx+dy*dy;
        double t=length<1e-16?0:Math.max(0,Math.min(1,((p.x()-a.x())*dx+(p.y()-a.y())*dy)/length));
        return Math.hypot(p.x()-a.x()-t*dx,p.y()-a.y()-t*dy);
    }
    public static Assessment assess(Glyph glyph,List<Template> templates,Tolerance tolerance,double shapeLimit,double shapeGap){
        Template best=null;double first=Double.POSITIVE_INFINITY,second=Double.POSITIVE_INFINITY;
        for(var template:templates){double error=shapeError(glyph.strokes(),template,tolerance.minimumExtent());
            if(error<first){second=first;first=error;best=template;}else second=Math.min(second,error);}
        if(best==null)return new Assessment(null,first,false,false);
        // Shape ambiguity cannot be overridden by a convenient drawing sequence.
        if(second-first<shapeGap)return new Assessment(null,first,true,false);
        var ordered=recognize(glyph,templates,tolerance);
        boolean precise=ordered.template()!=null&&ordered.template().id().equals(best.id());
        // A clear ordered/directional match uses its own configured tolerance. The
        // stricter shape-only threshold must not veto it (notably Heat's narrow bends).
        if(precise)return new Assessment(best,ordered.error(),false,true);
        if(first>shapeLimit)return new Assessment(null,first,false,false);
        return new Assessment(best,first,false,false);
    }
    public static List<Point> resample(List<Point> points,int samples) {
        if(points.size()<2||samples<2)throw new IllegalArgumentException("Stroke needs two points");
        double[] lengths=new double[points.size()];
        for(int i=1;i<points.size();i++)lengths[i]=lengths[i-1]+distance(points.get(i),points.get(i-1));
        var out=new ArrayList<Point>();int segment=1;
        for(int sample=0;sample<samples;sample++) {
            double target=lengths[lengths.length-1]*sample/(samples-1.0);
            while(segment<points.size()-1&&lengths[segment]<target)segment++;
            double span=lengths[segment]-lengths[segment-1],t=span<=1e-12?0:(target-lengths[segment-1])/span;
            var a=points.get(segment-1);var b=points.get(segment);
            out.add(new Point(Math.max(0,Math.min(1,a.x()+(b.x()-a.x())*t)),Math.max(0,Math.min(1,a.y()+(b.y()-a.y())*t))));
        }
        return List.copyOf(out);
    }
    private static double distance(Point a,Point b){return Math.hypot(a.x()-b.x(),a.y()-b.y());}
    private static double length(List<Point> stroke){double sum=0;for(int i=1;i<stroke.size();i++)sum+=distance(stroke.get(i-1),stroke.get(i));return sum;}
    private static List<List<Point>> normalized(List<List<Point>> strokes,double minimumExtent) {
        double minX=1,minY=1,maxX=0,maxY=0;
        for(var stroke:strokes)for(var point:stroke){minX=Math.min(minX,point.x());minY=Math.min(minY,point.y());maxX=Math.max(maxX,point.x());maxY=Math.max(maxY,point.y());}
        double scale=Math.max(maxX-minX,maxY-minY);if(scale<minimumExtent)return List.of();
        var out=new ArrayList<List<Point>>();
        for(var stroke:strokes){var normalized=new ArrayList<Point>();for(var point:stroke)normalized.add(new Point((point.x()-minX)/scale,(point.y()-minY)/scale));out.add(resample(normalized,24));}
        return out;
    }
    private static double turn(List<Point> points,int i){
        var a=points.get(i-1);var b=points.get(i);var c=points.get(i+1);
        double ax=b.x()-a.x(),ay=b.y()-a.y(),bx=c.x()-b.x(),by=c.y()-b.y();
        return Math.hypot(ax,ay)<1e-8||Math.hypot(bx,by)<1e-8?0:Math.atan2(ax*by-ay*bx,ax*bx+ay*by);
    }
    public static double error(List<List<Point>> strokes,Template template){return error(strokes,template,Tolerance.defaults(.12,.025));}
    public static double error(List<List<Point>> strokes,Template template,Tolerance tolerance) {
        if(strokes.size()!=template.strokes().size())return Double.POSITIVE_INFINITY;
        var a=normalized(strokes,tolerance.minimumExtent());var b=normalized(template.strokes(),.000001);
        if(a.isEmpty()||b.isEmpty())return Double.POSITIVE_INFINITY;
        double path=0,endpoints=0,corners=0,lengths=0,totalA=a.stream().mapToDouble(AonRecognition::length).sum(),totalB=b.stream().mapToDouble(AonRecognition::length).sum();
        for(int stroke=0;stroke<a.size();stroke++){
            var p=a.get(stroke);var q=b.get(stroke);
            for(int point=0;point<24;point++)path+=distance(p.get(point),q.get(point));
            endpoints+=distance(p.get(0),q.get(0))+distance(p.get(23),q.get(23));
            for(int point=1;point<23;point++){double difference=turn(p,point)-turn(q,point);corners+=Math.abs(Math.atan2(Math.sin(difference),Math.cos(difference)))/Math.PI;}
            lengths+=Math.abs(length(p)/Math.max(1e-9,totalA)-length(q)/Math.max(1e-9,totalB));
        }
        double weight=tolerance.pathWeight()+tolerance.endpointWeight()+tolerance.cornerWeight()+tolerance.lengthWeight();
        return (path/(a.size()*24)*tolerance.pathWeight()+endpoints/(a.size()*2)*tolerance.endpointWeight()
            +corners/(a.size()*22)*tolerance.cornerWeight()+lengths/a.size()*tolerance.lengthWeight())/weight;
    }
    public static Match recognize(Glyph glyph,List<Template> templates,double maximumError,double ambiguityGap){return recognize(glyph,templates,Tolerance.defaults(maximumError,ambiguityGap));}
    public static Match recognize(Glyph glyph,List<Template> templates,Tolerance tolerance) {
        Template best=null;double first=Double.POSITIVE_INFINITY,second=Double.POSITIVE_INFINITY;
        for(var template:templates){double error=error(glyph.strokes(),template,tolerance);if(error<first){second=first;first=error;best=template;}else second=Math.min(second,error);}
        if(best==null||first>tolerance.maximumError())return new Match(null,first,false);
        boolean ambiguous=second-first<tolerance.ambiguityGap();
        return new Match(ambiguous?null:best,first,ambiguous);
    }
    private AonRecognition() {}
}
