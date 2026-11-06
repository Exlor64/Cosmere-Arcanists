package dev.cosmerearcanists.aondor;
import java.util.*;

/** Pure typed expression compiler; no world mutation occurs until the complete program is valid. */
public record AonProgram(Node root,double quality) {
    public enum Type {ENTITY,BLOCK,POSITION,DIRECTION,VECTOR,NUMBER,REGION,ENTITIES,LIST,BOOLEAN,SKIP,VALUE,EFFECT}
    public record Node(String id,double number,Type type,List<Node> arguments){}
    public static AonProgram compile(AonDrawing drawing,List<AonRecognition.Template> templates,AonRecognition.Tolerance tolerance,double shapeError,double gap,double fallback){
        if(!drawing.glyphs().get(0).effect())throw new IllegalArgumentException("Draw the main effect first");
        int[] parents=AonLayout.parents(drawing);var quality=new double[]{1};return new AonProgram(node(drawing,templates,tolerance,shapeError,gap,fallback,parents,0,quality),quality[0]);
    }
    private static Node node(AonDrawing drawing,List<AonRecognition.Template> templates,AonRecognition.Tolerance tolerance,double error,double gap,double fallback,int[] parents,int index,double[] quality){
        var glyph=drawing.glyphs().get(index);var match=AonRecognition.assess(glyph,templates,tolerance,error,gap);
        if(match.template()==null)throw new IllegalArgumentException("Aon "+(index+1)+(match.ambiguous()?" is ambiguous.":" is not recognized."));
        if(match.template().effect()!=glyph.effect())throw new IllegalArgumentException(index==0?"Main Aon must be an effect":"An argument must be a targeting Aon");
        quality[0]=Math.min(quality[0],match.potency(fallback));var args=new ArrayList<Node>();
        for(int child:AonLayout.children(drawing,parents,index))args.add(node(drawing,templates,tolerance,error,gap,fallback,parents,child,quality));
        String id=match.template().id();String key=id.startsWith("cosmerearcanists:")?id.substring(17):id;
        Type type=switch(key){
            case "skip"->{arity(args,0,0,key);yield Type.SKIP;}
            case "caster","gaze_entity"->{arity(args,0,0,key);yield Type.ENTITY;}
            case "gaze_block"->{arity(args,0,0,key);yield Type.BLOCK;}
            case "position_of"->{arity(args,1,1,key);expect(args.get(0),key,Type.ENTITY,Type.BLOCK);yield Type.POSITION;}
            case "look_direction"->{arity(args,0,1,key);if(supplied(args,0))expect(args.get(0),key,Type.ENTITY);yield Type.DIRECTION;}
            case "number"->{arity(args,0,0,key);yield Type.NUMBER;}
            case "add","subtract","multiply","divide","vector","length","normalize"->AonMath.resultType(key,args);
            case "list","append","remove_at","at","size","intersection","union","difference","contains","if","and","or","not"->AonLists.resultType(key,args);
            case "sphere"->{arity(args,2,2,key);expect(args.get(0),key,Type.POSITION);expect(args.get(1),key,Type.NUMBER);yield Type.REGION;}
            case "entities_in"->{arity(args,1,1,key);expect(args.get(0),key,Type.REGION);yield Type.ENTITIES;}
            case "remember"->{arity(args,0,2,key);if(supplied(args,0))expect(args.get(0),key,Type.NUMBER);if(args.size()>1&&args.get(1).type()==Type.EFFECT)throw new IllegalArgumentException("Remember stores a value, not an effect");yield Type.EFFECT;}
            case "recall"->{arity(args,0,1,key);if(supplied(args,0))expect(args.get(0),key,Type.NUMBER);yield Type.VALUE;}
            case "exclude"->{arity(args,2,2,key);expect(args.get(0),key,Type.ENTITIES);expect(args.get(1),key,Type.ENTITY,Type.ENTITIES);yield Type.ENTITIES;}
            case "players","mobs"->{arity(args,1,1,key);expect(args.get(0),key,Type.ENTITIES);yield Type.ENTITIES;}
            case "nearest"->{arity(args,1,2,key);expect(args.get(0),key,Type.ENTITIES);if(supplied(args,1))expect(args.get(1),key,Type.POSITION);yield Type.ENTITY;}
            case "light"->{arity(args,0,1,key);if(supplied(args,0))expect(args.get(0),key,Type.POSITION,Type.ENTITY,Type.ENTITIES);yield Type.EFFECT;}
            case "force"->{arity(args,0,2,key);if(supplied(args,0))expect(args.get(0),key,Type.ENTITY,Type.ENTITIES);if(supplied(args,1))expect(args.get(1),key,Type.DIRECTION,Type.VECTOR);yield Type.EFFECT;}
            case "heat"->{arity(args,0,2,key);if(supplied(args,0))expect(args.get(0),key,Type.ENTITY,Type.ENTITIES,Type.BLOCK);if(supplied(args,1))expect(args.get(1),key,Type.NUMBER);yield Type.EFFECT;}
            default->throw new IllegalArgumentException("No operation configured for "+id);
        };
        var result=new Node(id,key.equals("number")?match.number():glyph.number(),type,List.copyOf(args));
        if(AonMath.operation(id))AonMath.constant(result);
        return result;
    }
    public static String signature(String id){if(id==null)return "Draw an Aon to identify it";return switch(id){
        case "cosmerearcanists:caster"->"Caster() → Entity";case "cosmerearcanists:gaze_entity"->"Gaze Entity() → Entity";case "cosmerearcanists:gaze_block"->"Gaze Block() → Block";
        case "cosmerearcanists:position_of"->"Position Of(Entity / Block) → Position";case "cosmerearcanists:look_direction"->"Look Direction([Entity]) → Direction";case "cosmerearcanists:number"->"Number: center = 0; outer circles = 1–10";
        case "cosmerearcanists:add"->"Add(A, B): Numbers, Vectors, or Position + Vector";
        case "cosmerearcanists:subtract"->"Subtract(A, B): A − B; positions yield a vector";
        case "cosmerearcanists:multiply"->"Multiply(A, B): Numbers or Vector × Number";
        case "cosmerearcanists:divide"->"Divide(A, B): Number / Number or Vector / Number";
        case "cosmerearcanists:vector"->"Vector(Number X, Number Y, Number Z)";
        case "cosmerearcanists:length"->"Length(Vector / Direction) → Number";
        case "cosmerearcanists:normalize"->"Normalize(Vector / Direction) → Direction";
        case "cosmerearcanists:sphere"->"Sphere(Position, Number radius) → Region";case "cosmerearcanists:entities_in"->"Entities In(Region) → Entity list";
        case "cosmerearcanists:skip"->"Skip() → omit an optional argument; use its default";
        case "cosmerearcanists:list"->"List([Values…]) → List (empty allowed)";
        case "cosmerearcanists:append"->"Append(List, Values…) → List";
        case "cosmerearcanists:remove_at"->"Remove At(List, Number index) → List; starts at 0";
        case "cosmerearcanists:at"->"At(List, Number index) → Value; starts at 0";
        case "cosmerearcanists:size"->"Size(List) → Number";
        case "cosmerearcanists:intersection"->"Intersection(List, List, …) → shared unique entries";
        case "cosmerearcanists:union"->"Union(List, List, …) → all unique entries";
        case "cosmerearcanists:difference"->"Difference(List, List, …) → first minus later lists";
        case "cosmerearcanists:contains"->"Contains(List, Value) → Boolean";
        case "cosmerearcanists:if"->"If(Boolean, Value when true, Value when false)";
        case "cosmerearcanists:and"->"And(Boolean, Boolean, …) → Boolean";
        case "cosmerearcanists:or"->"Or(Boolean, Boolean, …) → Boolean";
        case "cosmerearcanists:not"->"Not(Boolean) → Boolean";
        case "cosmerearcanists:remember"->"Remember([Number key = 0], [Value = Caster])";
        case "cosmerearcanists:recall"->"Recall([Number key = 0]) → stored value";
        case "cosmerearcanists:exclude"->"Exclude(Entity list, Entity / Entity list) → Entity list";
        case "cosmerearcanists:players"->"Players(Entity list) → Entity list";
        case "cosmerearcanists:mobs"->"Mobs(Entity list) → Entity list";
        case "cosmerearcanists:nearest"->"Nearest(Entity list, [Position; default caster]) → Entity";
        case "cosmerearcanists:light"->"Light([Position / Entity / Entity list])";case "cosmerearcanists:force"->"Force([Target], [Vector / Direction])";case "cosmerearcanists:heat"->"Heat([Entity / Entity list / Block], [Number power = 1])";default->id;};}
    public String calculations(){var values=new ArrayList<String>();collect(root,values);return values.isEmpty()?"":" Calculations: "+String.join("; ",values);}
    private static void collect(Node node,List<String> values){
        if(AonMath.operation(node.id())&&values.size()<3){Object value=AonMath.constant(node);if(value!=null){String id=node.id().substring(17);values.add(Character.toUpperCase(id.charAt(0))+id.substring(1)+" = "+(value instanceof AonMath.Vector v?"("+format(v.x())+", "+format(v.y())+", "+format(v.z())+")":format((Double)value)));}}
        for(var child:node.arguments())collect(child,values);
    }
    public static boolean supplied(List<Node> args,int index){return index<args.size()&&args.get(index).type()!=Type.SKIP;}
    private static String format(double value){String text=Double.toString(value);return text.endsWith(".0")?text.substring(0,text.length()-2):text;}
    private static void arity(List<Node> args,int min,int max,String id){if(args.size()<min||args.size()>max)throw new IllegalArgumentException(id+" expects "+(min==max?min:min+"–"+max)+" arguments; received "+args.size());}
    private static void expect(Node node,String id,Type... types){if(node.type()!=Type.VALUE&&Arrays.stream(types).noneMatch(t->t==node.type()||t==Type.ENTITIES&&node.type()==Type.LIST))throw new IllegalArgumentException(id+" received "+node.type()+"; expected "+Arrays.toString(types));}
}
