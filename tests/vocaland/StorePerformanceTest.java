package vocaland;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import vocaland.service.LocalData;
import vocaland.ui.store.StorePanel;
import vocaland.ui.store.StoreImageCache;
public class StorePerformanceTest {
 static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static StorePanel panel;
 static void category(String name)throws Exception{Method m=StorePanel.class.getDeclaredMethod("selectCategory",String.class);m.setAccessible(true);m.invoke(panel,name);}
 public static void main(String[]args)throws Exception{
  Path dir=Files.createTempDirectory("store-perf-");
  Files.copy(Path.of("data/items.txt"),dir.resolve("items.txt"));
  LocalData data=new LocalData(dir);data.login("test");
  for(var item:data.items())if(data.inventoryStates().containsKey(item.id())!=data.owns(item.id())||data.inventoryStates().getOrDefault(item.id(),false)!=data.equipped(item.id()))throw new AssertionError("Inventory mismatch");
  long begin=System.nanoTime();
  SwingUtilities.invokeAndWait(()->panel=new StorePanel(data,()->{}));
  System.out.printf("Store constructor: %.1f ms%n",(System.nanoTime()-begin)/1e6);
  boolean[] ready={false};
  for(int i=0;i<600&&!ready[0];i++){
   SwingUtilities.invokeAndWait(()->{try{ready[0]=(boolean)field(panel,"imagesReady");}catch(Exception e){throw new RuntimeException(e);}});
   if(!ready[0])Thread.sleep(25);
  }
  if(!ready[0])throw new AssertionError("Image worker did not finish");
  SwingUtilities.invokeAndWait(()->{try{
   StoreImageCache cache=(StoreImageCache)field(panel,"imageCache");int cacheSize=cache.size();
   if(cacheSize!=38)throw new AssertionError("Missing cached images: "+cacheSize);
   long start=System.nanoTime();
   for(int i=0;i<30;i++)for(String name:new String[]{"캐릭터","농구","배경"})category(name);
   double ms=(System.nanoTime()-start)/1e6;
   if(cache.size()!=cacheSize)throw new AssertionError("Cache changed on tab switches");
   category("캐릭터");
   JPanel grid=(JPanel)field(panel,"gridPanel");Component first=grid.getComponent(0),second=grid.getComponent(1);
   for(MouseListener listener:second.getMouseListeners())listener.mouseClicked(new MouseEvent(second,MouseEvent.MOUSE_CLICKED,0,0,10,10,1,false));
   if(grid.getComponent(0)!=first||grid.getComponent(1)!=second)throw new AssertionError("Cards recreated on selection");
   if(((JLabel)field(panel,"previewImageLabel")).getIcon()==null)throw new AssertionError("Missing preview");
   panel.refreshData();
   if(cache.size()!=cacheSize)throw new AssertionError("Cache changed on reentry");
   System.out.printf("PASS: %d cached variants; 90 tab switches %.1f ms (%.2f ms each); selection preserves cards; preview and refresh work.%n",cacheSize,ms,ms/90);
  }catch(Exception e){throw new RuntimeException(e);}});
 }
}
