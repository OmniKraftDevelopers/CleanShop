package net.omnikraft.CleanShop.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.omnikraft.CleanShop.CleanShop;
import net.omnikraft.CleanShop.chest.ChestData;
import net.omnikraft.CleanShop.shop.Shop;

public class FileHandler {
	
	public static CleanShop plugin;
	static int tickMethodID;
	public static boolean loadedFile=false;
	
	public static void copyFile(File in, File out) throws IOException {
		   FileChannel inChannel = new
		     FileInputStream(in).getChannel();
		  FileChannel outChannel = new
		      FileOutputStream(out).getChannel();
		    try {
	        	 inChannel.transferTo(0, inChannel.size(), outChannel);
		  } 
		 catch (IOException e) {
		      throw e;
		 }
		 finally {
		  if (inChannel != null) inChannel.close();
		  if (outChannel != null) outChannel.close();
		 }
 	}
	
	public static void saveShops()
	{
		CleanShop.log("Saving shops.");
		if(!getTempShopFile().exists())
		try {
			getTempShopFile().createNewFile();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
	              new FileOutputStream(getTempShopFile()), "utf-8"))) {
			
			JSONObject obj=new JSONObject();
			JSONArray shopObjs=new JSONArray();
			for(Shop s:ShopUtils.shops)
			{
				JSONObject shop=new JSONObject();
				shop.put("name", s.getRegion().getId());
				try{
 				shop.put("world", s.getLocation().getWorld().getName());
 				shop.put("x", (double)s.getLocation().getX());
 				shop.put("y", (double)s.getLocation().getY());
 				shop.put("z", (double)s.getLocation().getZ());
 				shop.put("pitch", (double)s.getLocation().getPitch());
 				shop.put("yaw", (double)s.getLocation().getYaw());
				}
				catch(NullPointerException e)
				{
					Bukkit.getLogger().log(Level.WARNING,"Shop "+s.getRegion().getId()+" doesn't have a tp location set. I'm setting it to the midpoint of the region for now.");
					BlockVector max=s.getRegion().getMaximumPoint();
					BlockVector min=s.getRegion().getMinimumPoint();
 				shop.put("world", "world");
 				shop.put("x", (double)(max.getBlockX()+min.getBlockX())/2d);
 				shop.put("y", (double)(max.getBlockY()+min.getBlockY())/2d);
 				shop.put("z", (double)(max.getBlockZ()+min.getBlockZ())/2d);
 				shop.put("pitch", 0.0);
 				shop.put("yaw", 0.0);
				}
				catch(Exception e)
				{
					System.out.println("Oh crap. Bad things happened.");
					e.printStackTrace();
				}
				
				JSONArray chestObjs=new JSONArray();
				for(ChestData c:s.chestData)
				{
					//System.out.println(c);
					JSONObject chest=new JSONObject();
					chest.put("x", (int)c.x);
					chest.put("y", (int)c.y);
					chest.put("z", (int)c.z);
 				JSONArray mats=new JSONArray();
 				for(Material m:c.getItems())
 					if(m!=null)
 						mats.add(m.name());
 				chest.put("items", mats);
 				chestObjs.add(chest);
				}
				shop.put("chests", chestObjs);
				shopObjs.add(shop);
			}
			obj.put("shops", shopObjs);
			obj.put("shopScan", CleanShop.shopScan);
			obj.put("debug", CleanShop.debug);
			
			writer.write(obj.toString());
			tickMethodID = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
 		    public void run() {
	 			try {
					copyFile(getTempShopFile(), getShopFile());
				} catch (IOException e) {
					e.printStackTrace();
				}
 		    }
 		    }, 1);
		} catch (Exception e) {
		e.printStackTrace();
	}
		CleanShop.log("Saved shops.");
	}

	public static File getShopFile()
	{
		return new File(plugin.getDataFolder().getAbsolutePath(),"shops.data");
	}
	
	public static File getTempShopFile()
	{
		return new File(plugin.getDataFolder().getAbsolutePath(),"shops_temp.data");
	}

	public static void loadShops()
		{
		ShopUtils.updateRegionManagers(plugin);
		CleanShop.log("Loading shops from file...");
			loadedFile=true;
			if(!getShopFile().exists())
				return;
			String line;
			String all="";
			try (
			    InputStream fis = new FileInputStream(getShopFile());
			    InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
			    BufferedReader br = new BufferedReader(isr);
			)
			{
				while ((line = br.readLine()) != null) {
			        all+=line;
			    }
			 JSONParser parser=new JSONParser();
			 JSONObject obj=(JSONObject)parser.parse(all);
			 CleanShop.shopScan=((boolean)obj.get("shopScan"));
			 JSONArray shops = (JSONArray)obj.get("shops");
			 CleanShop.debug=(boolean)obj.get("debug");
			 for(int i=0;i<shops.size();i++)
			 {
				 JSONObject shop=(JSONObject)shops.get(i);
	         World w=Bukkit.getWorld((String)shop.get("world"));
	         ProtectedRegion region=ShopUtils.getRegion((String)shop.get("name"));
		     ShopUtils.createShop(region);
		     Shop s=ShopUtils.getShop(region);
	
		     //This is where it gets ugly.
		     if(s!=null)
		     {
		    	 double x=0;
		    	 double y=0;
		    	 double z=0;
		    	 float yaw=0;
		    	 float pitch=0;
		    	 
			     if(shop.get("x") instanceof Double)
			    	 x=(double)shop.get("x");
			     else
			    	 x=Integer.parseInt(shop.get("x").toString());
		    	 
			     if(shop.get("y") instanceof Double)
			    	 y=(double)shop.get("y");
			     else
			    	 y=Integer.parseInt(shop.get("y").toString());
		    	 
			     if(shop.get("z") instanceof Double)
			    	 z=(double)shop.get("z");
			     else
			    	 z=Integer.parseInt(shop.get("z").toString());
		    	 
			     if(shop.get("yaw") instanceof Double)
			    	 yaw=(float)(double)shop.get("yaw");
			     else
			    	 yaw=Integer.parseInt(shop.get("yaw").toString());
		    	 
			     if(shop.get("pitch") instanceof Double)
			    	 pitch=(float)(double)shop.get("pitch");
			     else
			    	 pitch=Integer.parseInt(shop.get("pitch").toString());
	
		    	 CleanShop.log("Loaded shop "+(String)shop.get("name")+" in world "+(String)shop.get("world"));
			     s.setLocation(new Location(w,x,y,z,yaw,pitch));
		     }
		     else
		    	 CleanShop.log("Couldn't load shop from region "+(String)shop.get("name")+" in world "+(String)shop.get("world"));
		     
		     JSONArray chests=(JSONArray)shop.get("chests");
		     for(int j=0;j<chests.size();j++)
		     {
		    	 JSONObject chest=(JSONObject)chests.get(j);
		    	 ChestData c=new ChestData(Math.toIntExact((long)chest.get("x")),Math.toIntExact((long)chest.get("y")),Math.toIntExact((long)chest.get("z")),null);
		    	 JSONArray mats=(JSONArray)chest.get("items");
		    	 Material[] materials=new Material[mats.size()];
		    	 for(int k=0;k<mats.size();k++)
		    	 {
		    		 materials[k]=Material.getMaterial((String)mats.get(k));
		    	 }
		    	 c.setItems(materials);
		    	 s.chestData.add(c);
		     }
			 }
			} catch (Exception e) {
			System.err.println("There was an error loading the file.");
			e.printStackTrace();
		}
			CleanShop.log("Done loading shops.");
	}

}
