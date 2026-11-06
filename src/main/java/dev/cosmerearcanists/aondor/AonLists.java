package dev.cosmerearcanists.aondor;

import java.util.*;
import static dev.cosmerearcanists.aondor.AonProgram.*;

/** Pure bounded list/set operations; set outputs preserve first-encounter order. */
public final class AonLists {
    public static final int MAX_ENTRIES=128,MAX_DEPTH=8,MAX_VALUES=512;
    private static final Set<String> OPERATIONS=Set.of("list","append","remove_at","at","size","intersection","union","difference","contains","if","and","or","not","equal","greater_than","less_than");
    public static boolean operation(String id){return id.startsWith("cosmerearcanists:")&&OPERATIONS.contains(id.substring(17));}
    private static void count(String key,List<Node> args,int min,int max){if(args.size()<min||args.size()>max)throw new IllegalArgumentException(key+" expects "+min+(min==max?"":"–"+max)+" arguments");}
    private static void expect(Node node,Type... types){if(node.type()!=Type.VALUE&&Arrays.stream(types).noneMatch(t->t==node.type()))throw new IllegalArgumentException("List/logic argument is "+node.type()+"; expected "+Arrays.toString(types));}
    public static Type resultType(String key,List<Node> args){
        switch(key){
            case "equal"->{count(key,args,2,2);for(var n:args)if(n.type()==Type.EFFECT||n.type()==Type.SKIP)throw new IllegalArgumentException("Equal needs actual values");return Type.BOOLEAN;}
            case "greater_than","less_than"->{count(key,args,2,2);for(var n:args)expect(n,Type.NUMBER);return Type.BOOLEAN;}
            case "list"->{count(key,args,0,16);for(var n:args)if((n.type()==Type.EFFECT||n.type()==Type.SKIP))throw new IllegalArgumentException("A List contains values, not effects");return Type.LIST;}
            case "append"->{count(key,args,2,16);expect(args.get(0),Type.LIST,Type.ENTITIES);for(var n:args.subList(1,args.size()))if((n.type()==Type.EFFECT||n.type()==Type.SKIP))throw new IllegalArgumentException("Append takes values, not effects");return Type.LIST;}
            case "remove_at","at"->{count(key,args,2,2);expect(args.get(0),Type.LIST,Type.ENTITIES);expect(args.get(1),Type.NUMBER);return key.equals("at")?(args.get(0).type()==Type.ENTITIES?Type.ENTITY:Type.VALUE):args.get(0).type();}
            case "size"->{count(key,args,1,1);expect(args.get(0),Type.LIST,Type.ENTITIES);return Type.NUMBER;}
            case "union","intersection","difference"->{count(key,args,2,16);for(var n:args)expect(n,Type.LIST,Type.ENTITIES);return args.stream().allMatch(n->n.type()==Type.ENTITIES)?Type.ENTITIES:Type.LIST;}
            case "contains"->{count(key,args,2,2);expect(args.get(0),Type.LIST,Type.ENTITIES);if((args.get(1).type()==Type.EFFECT||args.get(1).type()==Type.SKIP))throw new IllegalArgumentException("Contains takes a value, not an effect");return Type.BOOLEAN;}
            case "if"->{count(key,args,3,3);expect(args.get(0),Type.BOOLEAN);if((args.get(1).type()==Type.EFFECT||args.get(1).type()==Type.SKIP)||(args.get(2).type()==Type.EFFECT||args.get(2).type()==Type.SKIP))throw new IllegalArgumentException("If selects values, not effects");return args.get(1).type()==args.get(2).type()?args.get(1).type():Type.VALUE;}
            case "and","or","not"->{count(key,args,key.equals("not")?1:2,key.equals("not")?1:16);for(var n:args)expect(n,Type.BOOLEAN);return Type.BOOLEAN;}
            default->throw new IllegalArgumentException("Unknown list operation "+key);
        }
    }
    public static int index(double index,int size){if(!Double.isFinite(index)||index<0||index!=Math.rint(index)||index>=size)throw new IllegalArgumentException("List index must be a whole number from 0 to "+(size-1));return (int)index;}
    public static <T> List<T> create(List<T> values){if(values.size()>MAX_ENTRIES)throw new IllegalArgumentException("List exceeds "+MAX_ENTRIES+" entries");return List.copyOf(values);}
    public static <T> List<T> append(List<T> list,List<T> entries){var result=new ArrayList<T>(list);result.addAll(entries);return create(result);}
    public static <T> T at(List<T> list,double index){return list.get(index(index,list.size()));}
    public static <T> List<T> removeAt(List<T> list,double index){var result=new ArrayList<T>(list);result.remove(index(index,list.size()));return create(result);}
    public static <T> List<T> sets(String operation,List<List<T>> lists){
        if(lists.size()<2||lists.size()>16)throw new IllegalArgumentException("Set operations take two or more lists (maximum16)");
        var result=new LinkedHashSet<T>(lists.get(0));
        for(var list:lists.subList(1,lists.size()))switch(operation){case "union"->result.addAll(list);case "intersection"->result.retainAll(list);case "difference"->result.removeAll(list);default->throw new IllegalArgumentException("Unknown set operation "+operation);}
        return create(new ArrayList<>(result));
    }
    private AonLists(){}
}
