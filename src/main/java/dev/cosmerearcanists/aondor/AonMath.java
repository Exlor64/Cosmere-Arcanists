package dev.cosmerearcanists.aondor;

import java.util.*;
import static dev.cosmerearcanists.aondor.AonProgram.*;

/** Pure finite double/vector arithmetic. Values are never rounded or scaled by drawing quality. */
public final class AonMath {
    public record Vector(double x,double y,double z){
        public Vector{finite(x);finite(y);finite(z);}
        public double length(){return finite(Math.hypot(x,Math.hypot(y,z)));}
    }
    public static boolean vector(Type type){return type==Type.VECTOR||type==Type.DIRECTION;}
    public static Type resultType(String operation,List<Node> args){
        int needed=operation.equals("vector")?3:operation.equals("length")||operation.equals("normalize")?1:2;
        if(args.size()!=needed)throw new IllegalArgumentException(operation+" expects "+needed+" arguments; received "+args.size());
        var a=args.get(0).type();var b=args.size()>1?args.get(1).type():null;
        if(operation.equals("vector")){if(args.stream().anyMatch(n->n.type()!=Type.NUMBER))throw new IllegalArgumentException("Vector expects three Numbers (X, Y, Z)");return Type.VECTOR;}
        if(operation.equals("length")||operation.equals("normalize")){if(!vector(a))throw new IllegalArgumentException(operation+" expects a Vector or Direction");return operation.equals("length")?Type.NUMBER:Type.DIRECTION;}
        if(a==Type.NUMBER&&b==Type.NUMBER&&Set.of("add","subtract","multiply","divide").contains(operation))return Type.NUMBER;
        return switch(operation){
            case "add"->{if(vector(a)&&vector(b))yield Type.VECTOR;if(a==Type.POSITION&&vector(b)||vector(a)&&b==Type.POSITION)yield Type.POSITION;throw types(operation,a,b);}
            case "subtract"->{if(vector(a)&&vector(b)||a==Type.POSITION&&b==Type.POSITION)yield Type.VECTOR;if(a==Type.POSITION&&vector(b))yield Type.POSITION;throw types(operation,a,b);}
            case "multiply"->{if(vector(a)&&b==Type.NUMBER||a==Type.NUMBER&&vector(b))yield Type.VECTOR;throw types(operation,a,b);}
            case "divide"->{if(vector(a)&&b==Type.NUMBER)yield Type.VECTOR;throw types(operation,a,b);}
            default->throw new IllegalArgumentException("Unknown math operation "+operation);
        };
    }
    private static IllegalArgumentException types(String op,Type a,Type b){return new IllegalArgumentException(op+" does not accept "+a+" and "+b);}
    public static Object apply(String op,Object... args){
        if(op.equals("vector"))return new Vector((Double)args[0],(Double)args[1],(Double)args[2]);
        if(op.equals("length"))return ((Vector)args[0]).length();
        if(op.equals("normalize")){var v=(Vector)args[0];double length=v.length();if(length==0)throw new IllegalArgumentException("Cannot normalize a zero vector");return new Vector(v.x()/length,v.y()/length,v.z()/length);}
        Object a=args[0],b=args[1];
        if(a instanceof Double x&&b instanceof Double y){finite(x);finite(y);return finite(switch(op){case "add"->x+y;case "subtract"->x-y;case "multiply"->x*y;case "divide"->{if(y==0)throw new IllegalArgumentException("Cannot divide by zero");yield x/y;}default->throw new IllegalArgumentException("Unknown math operation "+op);});}
        if(a instanceof Vector x&&b instanceof Vector y)return switch(op){case "add"->new Vector(x.x()+y.x(),x.y()+y.y(),x.z()+y.z());case "subtract"->new Vector(x.x()-y.x(),x.y()-y.y(),x.z()-y.z());default->throw new IllegalArgumentException("Unsupported vector operation "+op);};
        if(op.equals("multiply")){if(a instanceof Double x&&b instanceof Vector y)return scale(y,x);if(a instanceof Vector x&&b instanceof Double y)return scale(x,y);}
        if(op.equals("divide")&&a instanceof Vector x&&b instanceof Double y){finite(y);if(y==0)throw new IllegalArgumentException("Cannot divide by zero");return new Vector(finite(x.x()/y),finite(x.y()/y),finite(x.z()/y));}
        throw new IllegalArgumentException("Unsupported operands for "+op);
    }
    private static Vector scale(Vector v,double scale){finite(scale);return new Vector(v.x()*scale,v.y()*scale,v.z()*scale);}
    private static double finite(double value){if(!Double.isFinite(value))throw new IllegalArgumentException("Calculation is not finite (overflow or invalid value)");return value;}
    public static boolean operation(String id){return Set.of("cosmerearcanists:add","cosmerearcanists:subtract","cosmerearcanists:multiply","cosmerearcanists:divide","cosmerearcanists:vector","cosmerearcanists:length","cosmerearcanists:normalize").contains(id);}
    /** Null means the expression depends on the world; fixed errors still fail before casting. */
    public static Object constant(Node node){return constant(node,new int[]{AonDrawing.MAX_GLYPHS});}
    private static Object constant(Node node,int[] budget){
        if(--budget[0]<0)throw new IllegalArgumentException("Numeric expression exceeds the evaluation budget");
        if(node.id().equals("cosmerearcanists:number"))return finite(node.number());
        if(!operation(node.id()))return null;
        var values=new Object[node.arguments().size()];boolean known=true;
        for(int i=0;i<values.length;i++){values[i]=constant(node.arguments().get(i),budget);known&=values[i]!=null;}
        String op=node.id().substring(17);if(op.equals("divide")&&values.length==2&&values[1] instanceof Double divisor&&divisor==0)throw new IllegalArgumentException("Cannot divide by zero");
        return known?apply(op,values):null;
    }
    private AonMath(){}
}
