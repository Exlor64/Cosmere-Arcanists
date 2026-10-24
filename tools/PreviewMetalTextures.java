import dev.cosmerearcanists.visual.MetalPalette;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.regex.*;
import javax.imageio.ImageIO;
/** Draw a contact sheet from the production palette and vial model, without changing source sprites. */
public class PreviewMetalTextures {
    static final String[] TYPES={"spike","shavings","ingot","vial"};
    static boolean[][] vialMask(Path root)throws Exception{
        String text=Files.readString(root.resolve("src/main/resources/assets/cosmerearcanists/models/item/provided_vial.json"));
        var pattern=Pattern.compile("\\\"from\\\"\\s*:\\s*\\[\\s*(\\d+),\\s*(\\d+),");var matches=pattern.matcher(text);var starts=new java.util.ArrayList<Integer>();var coords=new java.util.ArrayList<int[]>();
        while(matches.find()){starts.add(matches.start());coords.add(new int[]{Integer.parseInt(matches.group(1)),15-Integer.parseInt(matches.group(2))});}
        boolean[][] result=new boolean[16][16];for(int i=0;i<starts.size();i++){String element=text.substring(starts.get(i),i+1<starts.size()?starts.get(i+1):text.length());int[] xy=coords.get(i);result[xy[0]][xy[1]]=element.contains("\"tintindex\": 0");}return result;
    }
    static void centered(Graphics2D g,String text,int x,int y){g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]),output=root.resolve("docs/metal-texture-examples.png");boolean[][] mask=vialMask(root);
        var canvas=new BufferedImage(1024,620,BufferedImage.TYPE_INT_ARGB);var g=canvas.createGraphics();g.setColor(new Color(0x10151C));g.fillRect(0,0,1024,620);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xF2F5F8));g.setFont(new Font("SansSerif",Font.BOLD,28));g.drawString("Metal texture examples",28,43);
        g.setFont(new Font("SansSerif",Font.PLAIN,15));g.setColor(new Color(0xAAB8C8));g.drawString("Copper (top) and gold (bottom) · original 16 × 16 artwork enlarged 8×",28,72);
        for(int column=0;column<4;column++){
            int x=28+246*column;g.setColor(new Color(0xD7E0EB));g.setFont(new Font("SansSerif",Font.BOLD,17));centered(g,new String[]{"SPIKES","SHAVINGS","INGOTS","VIALS"}[column],x+115,111);
            var source=ImageIO.read(root.resolve("src/main/resources/assets/cosmerearcanists/textures/item/provided_"+TYPES[column]+".png").toFile());
            for(int row=0;row<2;row++){
                String metal=row==0?"copper":"gold";int tint=MetalPalette.color(metal),y=130+234*row;
                g.setColor(new Color(0x2D3845));g.fillRect(x,y,230,212);g.setColor(new Color(0x1B232D));g.fillRect(x+1,y+1,228,210);
                int iconX=x+51,iconY=y+21;
                for(int py=0;py<16;py++)for(int px=0;px<16;px++){
                    int original=source.getRGB(px,py);boolean colored=column!=3||mask[px][py];int r=(original>>>16)&255,green=(original>>>8)&255,b=original&255;
                    if(colored){r=r*((tint>>>16)&255)/255;green=green*((tint>>>8)&255)/255;b=b*(tint&255)/255;}
                    g.setColor(new Color(r,green,b,(original>>>24)&255));g.fillRect(iconX+px*8,iconY+py*8,8,8);
                }
                g.setColor(new Color(tint,true));g.setFont(new Font("SansSerif",Font.BOLD,18));centered(g,row==0?"Copper":"Gold",x+115,y+176);
                g.setColor(new Color(0x94A4B7));g.setFont(new Font("SansSerif",Font.PLAIN,12));centered(g,column==3?"Glass and stopper unchanged":"Original shading preserved",x+115,y+196);
            }
        }
        g.setColor(new Color(0x8797AA));g.setFont(new Font("SansSerif",Font.PLAIN,12));g.drawString("Texture previews from the mod palette; not Minecraft screenshots.",28,606);g.dispose();ImageIO.write(canvas,"png",output.toFile());System.out.println(output);
    }
}
