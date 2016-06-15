package net.omnikraft.CleanShop;

import java.util.List;
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
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
 
public final class CleanShop extends JavaPlugin{
		public boolean loadedFile=false;
		public Vector<Shop> shops = new Vector<Shop>();
		public WorldGuardPlugin worldGuard;
		public RegionManager regionManager;
		public Vector<NameToID> nameToIDs = new Vector<NameToID>();
		int waitToCopyTimer=0;
		int tickMethodID;
		public boolean shopScan=true;

		public File getShopFile()
		{
			return new File(getDataFolder().getAbsolutePath(),"shops.data");
		}
		public File getTempShopFile()
		{
			return new File(getDataFolder().getAbsolutePath(),"shops_temp.data");
		}
		
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

	 	public void onEnable() {
	        getDataFolder().mkdirs();
	 		nameToIDs = new Vector<NameToID>();
	 		worldGuard=getWorldGuard();
	 		if(worldGuard==null)
	 		{
	 			getLogger().info(" No WorldGuard plugin found! CleanShop requires it, so it has been disabled automatically.");
	 			getServer().getPluginManager().disablePlugin(this);
	 			return;
	 		}
	 		getServer().getPluginManager().registerEvents(new EventListener(this), this);
	 		regionManager=worldGuard.getRegionManager(Bukkit.getWorlds().get(0));

			loadShops();
	    }
	 	
		public void saveShops()
	 	{
			//System.out.println("saving");
	 		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
	 	              new FileOutputStream(getTempShopFile()), "utf-8"))) {
	 			if(!getTempShopFile().exists())
	 				getTempShopFile().createNewFile();
	 			
	 			JSONObject obj=new JSONObject();
 				JSONArray shopObjs=new JSONArray();
	 			for(Shop s:shops)
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
 				obj.put("shopScan", shopScan);
	 			
 				writer.write(obj.toString());
	 			tickMethodID = getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
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
	 	}
	 	
		public void loadShops()
	 	{
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
	 		 shopScan=((boolean)obj.get("shopScan"));
	 		 JSONArray shops = (JSONArray)obj.get("shops");
	 		 for(int i=0;i<shops.size();i++)
	 		 {
	 			 JSONObject shop=(JSONObject)shops.get(i);
		         World w=Bukkit.getWorld("world");
		         //System.out.println("-------- "+(String)shop.get("name"));
		         ProtectedRegion region=regionManager.getRegion((String)shop.get("name"));
			     this.createShop(region);
			     Shop s=getShop(region);

			     //This is where it gets ugly.
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
				     
				     s.setLocation(new Location(w,x,y,z,yaw,pitch));
			     }
			     
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
	 	}
	 	
	    public void onDisable() {
	    }
	    
	    private WorldGuardPlugin getWorldGuard() {
	        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
	     
	        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
	            return null;
	        }
	     
	        return (WorldGuardPlugin) plugin;
	    }
	    
	    public ApplicableRegionSet getRegions(Location location)
	    {
	    	return regionManager.getApplicableRegions(location);
	    }
	    
	    public String getPlayer(UUID id)
	    {
	    	for(NameToID n:nameToIDs)
	    	{
	    		if(n.uuid.equals(id))
	    		{
	    			//System.out.println("Found "+n.name+" in stored names");
	    			return n.name;
	    		}
	    	}
	    	String p=null;
			try {
				p = NameFetcher.getNameOf(id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//System.out.println("P: "+p);

			if(p!=null&&!p.equals(""))
				this.addNameUUID(id, p);
			else
				return null;
	    	return p;
	    }
	    
	    public Vector<Shop> getOwnedShops(Player player)
	    {
	    	Vector<Shop> ss=new Vector<Shop>();
	    	for(Shop s:shops)
	    	{
	    		if(s.getRegion().getMembers().getUniqueIds().contains(player.getUniqueId()))
	    		{
	    			ss.add(s);
	    		}
	    	}
	    	return ss;
	    }
	    
	    public void addNameUUID(UUID id, String name)
	    {
	    	for(NameToID n:nameToIDs)
	    	{
	    		if(n.uuid.equals(id))
	    			return;
	    	}
	    	nameToIDs.add(new NameToID(id,name));
	    }
	    
	    public void createShop(ProtectedRegion region)
	    {
	    	Shop shop = new Shop();
	    	shop.setRegion(region);
	    	shops.add(shop);
	    }

	    public boolean shopExists(ProtectedRegion r)
	    {
	    	for(Shop s:shops)
	    		if(s.getRegion().equals(r))
	    			return true;
	    	return false;
	    }
	    
	    public void setTeleport(Shop s,Location v)
	    {
	    	s.setLocation(v);
	    }
	    public Shop getShop(ProtectedRegion r)
	    {
	    	for(Shop s:shops)
	    		if(s.getRegion().equals(r))
	    			return s;
	    	return null;
	    }
	    
	    public void removeShop(ProtectedRegion r)
	    {
	    	Shop removed=null;
	    	for(Shop s:shops)
	    		if(s.getRegion().equals(r))
	    			removed=s;
	    	if(removed!=null)
	    		shops.remove(removed);
	    }
	    
	    public Vector<Shop> getShopsWithItem(Player player,String s)
	    {
			Material m = matchMaterial(s);
			Vector<Shop> ss=new Vector<Shop>();
			for(Shop sh:shops)
			{
				if(sh.hasItem(m))
					ss.add(sh);
			}
			return ss;
	    }
	    
	    public void clearSingleChest(Chest c, Shop s)
	    {
	    	ChestData rm=null;
			for(ChestData cd:s.chestData)
				if(cd.x==c.getX()&&cd.y==c.getY()&&cd.z==c.getZ())
				{
					rm=cd;
					break;
				}
			if(rm!=null)
				s.chestData.remove(rm);
	    }
	    
	    //uuuuuuuuugggggggggggghhhhhhhhhhhhhhhh
	    public void dealWithThisFreakingDoubleChest(Chest c, Shop s, BlockFace side,List<Block> blockList)
	    {
	    	ChestData rm=null;
			for(ChestData cd:s.chestData)
				if(cd.x==c.getX()&&cd.y==c.getY()&&cd.z==c.getZ())
				{
					rm=cd;
					break;
				}
			if(rm!=null)
				s.chestData.remove(rm);
			
			rm=null;
			int x=(int) c.getX();
			int z=(int) c.getZ();
			if(side==BlockFace.NORTH)
				z--;
			else if(side==BlockFace.SOUTH)
				z++;
			else if(side==BlockFace.EAST)
				x++;
			else if(side==BlockFace.WEST)
				x--;
			for(ChestData cd:s.chestData)
				if(cd.x==x&&cd.y==c.getY()&&cd.z==z)
				{
					rm=cd;
					break;
				}
			if(rm!=null)
				s.chestData.remove(rm);
			if(blockList==null||!blockList.contains(c.getWorld().getBlockAt(x, c.getY(), z)))
				calculateChestStock((Chest)c.getWorld().getBlockAt(x, c.getY(), z).getState(),s);
	    }
	    
	    public void calculateChestStock(Chest c,Shop s)
	    {
	    	ChestData data=s.getChestAt(c.getX(), c.getY(), c.getZ());
	    	if(data==null)
	    	{
	    		data=new ChestData(c.getX(), c.getY(), c.getZ(),null);
	    		s.chestData.add(data);
	    	}

	         invContents(c.getBlockInventory(),data);
	         //System.out.println("Single chest");
	    }
	    public void calculateChestStock(DoubleChest c,Shop s)
	    {
	    	ChestData data=s.getChestAt((int)c.getX(), (int)c.getY(), (int)c.getZ());
	    	if(data==null)
	    	{
	    		data=new ChestData((int)c.getX(), (int)c.getY(), (int)c.getZ(),null);
	    		s.chestData.add(data);
	    	}

	         invContents(c.getInventory(),data);
	        // System.out.println("Double chest");
	    }
	    
	    public void invContents(Inventory blockInv,ChestData data)
	    {
	    	Vector<Material> stock = new Vector<Material>();
	    	for(ItemStack i:blockInv.getContents())
            {
	           	 if(i!=null&&i.getType()!=null)
	           	 {
	           		 //Add every item that isn't named or a diamond.
	               	 if(!stock.contains(i.getType())&&
	               			 i.getItemMeta().getDisplayName()==null&&
	               			 i.getType()!=Material.DIAMOND)
	               	 {
	               		 stock.add(i.getType());
	               		 //System.out.println("Added "+i.getType());
	               	 }
	           	 }
            }
	    	Material[] dat=new Material[stock.size()];
	    	for(int i=0;i<stock.size();i++)	
	    	{
	    		dat[i]=stock.get(i);
	    	}
	    	data.setItems(dat);
	    }
	    
	   
	    
		public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	    	try{
	    	if (cmd.getName().equalsIgnoreCase("createshop")) {
	    		if(sender instanceof Player)
	    		{
	    			Player player = (Player)sender;
	    			if(args.length==0)
	    			{
		    			if(!player.getLocation().getWorld().getName().equals("world"))
		    			{
		    				player.sendMessage(ChatColor.RED+"You can only create shops in the overworld!");
		    				return true;
		    			}
		    			ApplicableRegionSet ars = getRegions(player.getLocation());
		    			Set<ProtectedRegion> regions=ars.getRegions();
		    			if(regions.size()==0)
		    			{
							sender.sendMessage(ChatColor.RED+"You need to be standing in a WorldGuard region to create a shop!");
							return true;
		    			}
		    			else if(regions.size()>1)
		    			{
							sender.sendMessage("You are standing in multiple regions and I don't know which one to use for the shop! Use /createShop <regionName> instead.");
							return true;
		    			}
		    			else
		    			{
		    				ProtectedRegion[] r = new ProtectedRegion[1];
		    				ProtectedRegion region=regions.toArray(r)[0];
		    				if(!shopExists(region))
	    					{
	    						createShop(region);
	    						sender.sendMessage("A shop has been added to region: "+region.getId());
	    						sender.sendMessage(ChatColor.YELLOW+"This shop's teleport has been set to your location. Use /sst to change it.");
	    						setShopTeleport(player, args);
	    			    		saveShops();
	    					}
	    					else
	    						sender.sendMessage(ChatColor.RED+"A shop already exists here!");
		    				return true;
		    			}
	    			}
	    			else if(args.length==1)
	    			{
	    				ProtectedRegion region = regionManager.getRegion(args[0]);
	    				if(region==null)
	    				{
							sender.sendMessage(ChatColor.RED+"No regions in the overworld could be found with name \""+args[0]+"\".");
							return true;
	    				}
	    				else
	    				{
	    					if(!shopExists(region))
	    					{
	    						createShop(region);
	    						sender.sendMessage("A shop has been added to region: "+args[0]);
	    			    		saveShops();
	    					}
	    					else
	    						sender.sendMessage(ChatColor.RED+"A shop already exists there!");
	    					return true;
	    				}
	    			}
	    		}
	    	}else if (cmd.getName().equalsIgnoreCase("listshops")) {
	    		sender.sendMessage("Here is a list of all shops: ");
	    		String msg="";
	    		Vector<Shop> noOwners=new Vector<Shop>();
	    		for(Shop s:shops)
	    		{
	    			Vector<String> owner=new Vector<String>();
	    			Set<UUID> ids = s.getRegion().getMembers().getUniqueIds();
	    			for(UUID id:ids)
						try {
							String p=getPlayer(id);
							if(p!=null&&!p.equals(""))
							{
								owner.add(p);
							}
						} catch (Exception e1) {
							e1.printStackTrace();
						}
	    			
	    			if(owner.size()==0)
	    			{
	    				noOwners.add(s);
	    				continue;
	    			}
	    			
	    			String owners="";
	    			for(String ss:owner)
	    			{
	    				//The catch is in case the owner name is null, I guess.
	    				try {
							owners+=ChatColor.AQUA+ss;
						} catch (Exception e) {
				    		sender.sendMessage("This is a thing you should never ever see. Tell the staff, bruh.");
							e.printStackTrace();
						}
	    				if(owner.size()>2&&ss.equals(owner.get(owner.size()-2)))
	    					owners+=ChatColor.WHITE+", and ";
	    				else if(owner.lastElement()!=ss)
	    					owners+=ChatColor.WHITE+", ";
	    			}
	    			msg+=ChatColor.WHITE+"{Shop "+ChatColor.GREEN+s.getRegion().getId()+ChatColor.WHITE+", owned by "+owners+ChatColor.WHITE+"} ";
	    		}
    			sender.sendMessage(msg);
    			String no = "";
    			for(Shop s:noOwners)
    				no+=s.getRegion().getId()+", ";
	    		if(noOwners.size()>0)
	    			sender.sendMessage("These shops have no owners: "+no);
    			return true;
	    	}else if (cmd.getName().equalsIgnoreCase("removeshop"))
	    	{
	    		if(sender instanceof Player)
	    		{
	    			if(args.length==1)
	    			{
	    				ProtectedRegion region = regionManager.getRegion(args[0]);
	    				boolean r=this.shopExists(region);
	    				removeShop(region);
	    				if(r)
	    				{
	    					sender.sendMessage("Shop "+args[0]+" removed.");
	    		    		saveShops();
	    				}
	    				else
	    					sender.sendMessage("No shop named "+args[0]+" exists.");
	    				return true;
	    			}
	    		}
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("searchshops")) {
	    		if(args.length==1)
	    		{
					if(matchMaterial(args[0])==null)
					{
						sender.sendMessage(ChatColor.RED+"That's not an item!");
					}
					else
					{
						Vector<Shop> theShops = this.getShopsWithItem((Player)sender,args[0]);
						sender.sendMessage("Shops that have "+ChatColor.BLUE+args[0]+ChatColor.WHITE+": ");
						Vector<String> msgs=new Vector<String>();
						for(Shop s:theShops)
						{
							Vector<String> owner=new Vector<String>();
			    			Set<UUID> ids = s.getRegion().getMembers().getUniqueIds();
			    			for(UUID id:ids)
								try {
									String p=getPlayer(id);
									if(p!=null&&!p.equals(""))
									{
										owner.add(p);
									}
								} catch (Exception e1) {
									e1.printStackTrace();
								}
			    			
			    			if(owner.size()==0)
			    			{
								sender.sendMessage("Shop "+ChatColor.GREEN+s.getRegion().getId()+ChatColor.WHITE+", owned by nobody, apparently.");
			    				continue;
			    			}
			    			
			    			String owners="";
			    			for(String ss:owner)
			    			{
			    				try {
									owners+=ChatColor.AQUA+ss;
								} catch (Exception e) {
						    		sender.sendMessage("Dude you should never ever see this. Tell the staff plx.");
									e.printStackTrace();
								}

			    				if(owner.size()>2&&ss.equals(owner.get(owner.size()-2)))
			    					owners+=ChatColor.WHITE+", and ";
			    				else if(!owner.lastElement().equals(ss))
			    					owners+=ChatColor.WHITE+", ";
			    			}
			    			msgs.add(ChatColor.WHITE+"Shop "+ChatColor.GREEN+s.getRegion().getId()+ChatColor.WHITE+", owned by "+owners+ChatColor.WHITE);
						}
						Collections.shuffle(msgs);
						for(String s:msgs)
							sender.sendMessage(s);
					}
	    		}
	    		else{
	    			if(args.length==0)
	    				sender.sendMessage(ChatColor.RED+"You need to provide an item to search for!");
					return false;
	    		}
	    		return true;
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("setshopteleport")||cmd.getName().equalsIgnoreCase("sst")) {
	    		if(sender instanceof Player)
	    		{
	    			if(((Player)sender).getLocation().getWorld().getName().equals("world"))
	    				return setShopTeleport((Player)sender,args);
	    			else
	    				sender.sendMessage(ChatColor.RED+"A shop's teleport must be in the overworld.");
	    		}
	    		saveShops();
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("reloadshops")) {
	    		sender.sendMessage(ChatColor.GRAY+"Reloading shops, please wait...");
	    		shops.clear();
	    		this.loadedFile=false;
	    		loadShops();
	    		sender.sendMessage("Shops have been reloaded!");
	    		return true;
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("setshopscan")) {
	    		if(args.length==1)
	    		{
	    			if(args[0].toLowerCase().equals("true"))
	    			{
	    				this.shopScan=true;
	    				sender.sendMessage(ChatColor.GREEN+"Enabled shop scanning.");
	    			}
	    			else if(args[0].toLowerCase().equals("false"))
	    			{
	    				shopScan=false;
	    				sender.sendMessage(ChatColor.GREEN+"Disabled shop scanning.");
	    			}
	    			else
	    			{
	    				sender.sendMessage(ChatColor.RED+"Argument must be true or false.");
	    				return false;
	    			}
    				saveShops();
    				return true;
	    		}
	    		else if(args.length==0)
	    			sender.sendMessage(ChatColor.GOLD+"Shop scanning is currently "+(shopScan?"enabled.":"disabled."));
	    		return false;
	    	}else if (cmd.getName().equalsIgnoreCase("wipestock"))
	    	{
	    		if(args.length==0)
	    		{
		    		for(Shop s:this.shops)
		    			s.chestData.clear();
		    		sender.sendMessage(ChatColor.GREEN+"All shop stocks have been wiped. Re-opening chests will refresh stock.");
					saveShops();
		    		return true;
	    		}
	    		else if(args.length==1)
	    		{
	    			for(Shop s:this.shops)
	    				if(s.getRegion().getId().toLowerCase().equals(args[0].toLowerCase()))
	    					s.chestData.clear();
		    		sender.sendMessage(ChatColor.GREEN+"The shop stocks of shop "+args[0]+" have been wiped. Re-opening chests will refresh stock.");
					saveShops();
		    		return true;
	    		}
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("tpshop")) {
	    		if(sender instanceof Player)
	    		{
	    			if(((Player)sender).hasPermission("cleanshop.tpshop"))
	    			{
			    		if(args.length==1)
			    		{
			    			Shop s=null;
			    			for(Shop ss:shops)
			    			{
			    				if(ss.getRegion().getId().equalsIgnoreCase(args[0]))
			    				{
			    					s=ss;
			    					break;
			    				}
			    			}
			    			
		    				if(s==null)
		    				{
		    					for(Shop sh:shops)
		    					{
		    						for(UUID i:sh.getRegion().getMembers().getUniqueIds())
		    						{
		    							//System.out.println(i);
		    							{
			    							String n=this.getPlayer(i);
			    							//System.out.println(n);
			    							if(n==null)
			    								continue;
			    							if(n.equalsIgnoreCase(args[0]))
			    							{
			    								s=sh;
			    								break;
			    							}
		    							}
		    						}
		    					}
		    				}
		    				if(s!=null)
		    				{
		    					if(s.getLocation()==null)
		    					{
		    						sender.sendMessage(args[0]+" doesn't have a teleport location set!");
		    						return true;
		    					}
		    					else
		    						((Player)sender).teleport(s.getLocation());
		    				}
		    				else
		    					sender.sendMessage(ChatColor.RED+"No shops or shopowners found with name "+args[0]+".");
			    			return true;
			    		}
	    			}
	    			else{
	    				sender.sendMessage(ChatColor.RED+"You don't have permission!");
	    			}
	    		}
	    	}
	    	return false; 
	    }catch(Exception e)
		{
			sender.sendMessage(ChatColor.RED+"CleanShops had an error!");
			e.printStackTrace();
		}
		return true;
	}
		
	public boolean setShopTeleport(Player player,String[] args)
	{

		if(args.length==0)
		{
		
			ApplicableRegionSet ars = getRegions(player.getLocation());
			Set<ProtectedRegion> regions=ars.getRegions();
			if(regions.size()==0)
			{
				player.sendMessage(ChatColor.RED+"You need to be standing in a WorldGuard region in the overworld to set the teleport! (Or you can use /setshopteleport [shop])");
				return true;
			}
			else if(regions.size()>1)
			{
				player.sendMessage("You are standing in multiple regions and I don't know which one to use for the shop! Use /setShopTeleport <regionName> instead.");
				return true;
			}
			else
			{
				ProtectedRegion[] r = new ProtectedRegion[1];
				ProtectedRegion region=regions.toArray(r)[0];
				
				Shop shop=this.getShop(region);
				if(shop!=null)
				{
					setTeleport(shop,player.getLocation());
					player.sendMessage("Set teleport for shop "+shop.getRegion().getId()+" to your location.");
					saveShops();
				}
				else
					player.sendMessage(ChatColor.RED+"Couldn't find that shop!");
				
				return true;
			}
		}
		else if(args.length==1)
		{
			ProtectedRegion region = regionManager.getRegion(args[0]);
			if(region==null)
			{
				player.sendMessage(ChatColor.RED+"No regions in the overworld could be found with name \""+args[0]+"\".");
				return true;
			}
			else
			{
				if(shopExists(region))
				{
					Shop shop=this.getShop(region);
					if(shop!=null)
					{
						setTeleport(shop,player.getLocation());
						player.sendMessage("Set teleport for shop "+shop.getRegion().getId()+" to your location.");
						saveShops();
					}
					else
						player.sendMessage(ChatColor.RED+"Couldn't find that shop!");
				}
				else
					player.sendMessage(ChatColor.RED+"No shop exists in region \""+args[0]+"\".");
				return true;
			}
		}
		return false;
	}

	public static Material matchMaterial(String s)
	{
		Material m= Material.matchMaterial(s);
	
		if(s.equalsIgnoreCase("iron_horse_armor"))
			return  Material.matchMaterial("iron_barding");
		if(s.equalsIgnoreCase("golden_horse_armor")||s.equalsIgnoreCase("gold_horse_armor"))
			return  Material.matchMaterial("gold_barding");
		if(s.equalsIgnoreCase("diamond_horse_armor"))
			return Material.matchMaterial("diamond_barding");
		
		return m;
	}
	
}
