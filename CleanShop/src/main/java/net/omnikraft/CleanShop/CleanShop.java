package net.omnikraft.CleanShop;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
 
public final class CleanShop extends JavaPlugin{
		public boolean loadedFile=false;
		public Vector<Shop> shops = new Vector<Shop>();
		public WorldGuardPlugin worldGuard;
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
		
		public void copyFile(File inputFile, File outputFile)
		{
			 InputStream inStream = null;
		        OutputStream outStream = null;
		        if(!outputFile.exists())
					try {
						outputFile.createNewFile();
					} catch (IOException e1) {
						e1.printStackTrace();
					}

		        try {
					inStream = new FileInputStream(inputFile);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
		        try {
					outStream = new FileOutputStream(outputFile);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

		        byte[] buffer = new byte[1024];


		        int fileLength;
		        try {
					while ((fileLength = inStream.read(buffer)) > 0){

					      outStream.write(buffer, 0, fileLength );

					      }
				} catch (IOException e) {
					e.printStackTrace();
				}

		        try {
					inStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		        try {
					outStream.close();
				} catch (IOException e) {
					e.printStackTrace();
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
	 				shop.put("world", s.getLocation().getWorld().getName());
	 				shop.put("x", s.getLocation().getX());
	 				shop.put("y", s.getLocation().getY());
	 				shop.put("z", s.getLocation().getZ());
	 				shop.put("pitch", s.getLocation().getPitch());
	 				shop.put("yaw", s.getLocation().getYaw());
	 				
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
			 			copyFile(getTempShopFile(), getShopFile());
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
	 		) { while ((line = br.readLine()) != null) {
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
		         ProtectedRegion region=worldGuard.getRegionManager(w).getRegion((String)shop.get("name"));
			     this.createShop(region);
			     Shop s=getShop(region);
			     
			     s.setLocation(new Location(w,
			    		 (double)shop.get("x"),
			    		 (double)shop.get("y"),
			    		 (double)shop.get("z"),
			    		 (float)Double.parseDouble(shop.get("yaw").toString())
			    		 ,(float)Double.parseDouble(shop.get("pitch").toString())));
			     
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
	    	return WGBukkit.getRegionManager(location.getWorld()).getApplicableRegions(location);
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
	    
	    public boolean hasItemPermissions(Player player, Shop shop)
	    {
	    	return hasMember(player,shop)||hasOwner(player,shop);
	    }
	    
	    public boolean hasMember(Player player,Shop shop)
	    {
	    	for(Shop s:shops)
	    	{
	    		if(shop.equals(s))
		    		if(s.getRegion().getMembers().getUniqueIds().contains(player.getUniqueId()))
		    		{
		    			return true;
		    		}
	    	}
	    	return false;
	    }
	    
	    public boolean hasOwner(Player player,Shop shop)
	    {
	    	for(Shop s:shops)
	    	{
	    		if(shop.equals(s))
		    		if(s.getRegion().getOwners().getUniqueIds().contains(player.getUniqueId()))
		    		{
		    			return true;
		    		}
	    	}
	    	return false;
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
	    public void dealWithThisFreakingDoubleChest(Chest c, Shop s, BlockFace side)
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
	    			if(args.length==0)
	    			{
		    			Player player = (Player)sender;
		    			ApplicableRegionSet ars = getRegions(player.getLocation());
		    			Set<ProtectedRegion> regions=ars.getRegions();
		    			if(regions.size()==0)
		    			{
							sender.sendMessage(ChatColor.RED+"You need to be standing in a WorldGuard protected region to create a shop!");
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
	    						sender.sendMessage(ChatColor.YELLOW+"Please note that you should set the location of the shop via /setshopteleport (or /sst) before the server is closed!");
	    			    		//saveShops();
	    					}
	    					else
	    						sender.sendMessage(ChatColor.RED+"A shop already exists here!");
		    				return true;
		    			}
	    			}
	    			else if(args.length==1)
	    			{
	    				ProtectedRegion region = worldGuard.getRegionManager(((Player) sender).getWorld()).getRegion(args[0]);
	    				if(region==null)
	    				{
							sender.sendMessage(ChatColor.RED+"No regions could be found with name \""+args[0]+"\".");
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
	    				ProtectedRegion region = worldGuard.getRegionManager(((Player) sender).getWorld()).getRegion(args[0]);
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
						String msg="";
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
			    			msg+=ChatColor.WHITE+"Shop "+ChatColor.GREEN+s.getRegion().getId()+ChatColor.WHITE+", owned by "+owners+ChatColor.WHITE;
							sender.sendMessage(msg);
							msg="";
						}
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
	    			if(args.length==0)
	    			{
		    			Player player = (Player)sender;
		    			ApplicableRegionSet ars = getRegions(player.getLocation());
		    			Set<ProtectedRegion> regions=ars.getRegions();
		    			if(regions.size()==0)
		    			{
							sender.sendMessage(ChatColor.RED+"You need to be standing in a WorldGuard region to set the teleport! (Or you can use /setshopteleport [shop])");
							return true;
		    			}
		    			else if(regions.size()>1)
		    			{
							sender.sendMessage("You are standing in multiple regions and I don't know which one to use for the shop! Use /setShopTeleport <regionName> instead.");
							return true;
		    			}
		    			else
		    			{
		    				ProtectedRegion[] r = new ProtectedRegion[1];
		    				ProtectedRegion region=regions.toArray(r)[0];
		    				
	    					Shop shop=this.getShop(region);
	    					if(shop!=null)
	    					{
	    						setTeleport(shop,((Player) sender).getLocation());
	    						sender.sendMessage("Set teleport for shop "+shop.getRegion().getId()+" to your location.");
	    						saveShops();
	    					}
	    					else
	    						sender.sendMessage(ChatColor.RED+"Couldn't find that shop!");
	    					
		    				return true;
		    			}
	    			}
	    			else if(args.length==1)
	    			{
	    				ProtectedRegion region = worldGuard.getRegionManager(((Player) sender).getWorld()).getRegion(args[0]);
	    				if(region==null)
	    				{
							sender.sendMessage(ChatColor.RED+"No regions could be found with name \""+args[0]+"\".");
							return true;
	    				}
	    				else
	    				{
	    					if(shopExists(region))
	    					{
	    						Shop shop=this.getShop(region);
		    					if(shop!=null)
		    					{
		    						setTeleport(shop,((Player) sender).getLocation());
		    						sender.sendMessage("Set teleport for shop "+shop.getRegion().getId()+" to your location.");
		    						saveShops();
		    					}
		    					else
		    						sender.sendMessage(ChatColor.RED+"Couldn't find that shop!");
	    					}
	    					else
	    						sender.sendMessage(ChatColor.RED+"No shop exists in region \""+args[0]+"\".");
	    					return true;
	    				}
	    			}
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
	    	else if (cmd.getName().equalsIgnoreCase("relocateshop")) {
	    		if(args.length==2)
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
	    				sender.sendMessage(ChatColor.RED+"That shop doesn't exist!");
	    			}
	    			else{
	    	    		ProtectedRegion newRegion=worldGuard.getRegionManager(((Player) sender).getWorld()).getRegion(args[1]);
	    	    		if(newRegion==null)
	    	    			sender.sendMessage(ChatColor.RED+"No region in this dimension exists with name "+args[1]);
	    	    		else
	    	    		{
	    	    			sender.sendMessage("Successfully relocated shop "+args[0]+" to "+args[1]+".");
	    	    			s.setRegion(newRegion);
	    	    			saveShops();
	    	    		}
	    			}
    				return true;
	    		}
	    		return false;
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
	    		for(Shop s:this.shops)
	    			s.chestData.clear();
	    		sender.sendMessage(ChatColor.GREEN+"All shop stocks have been wiped. Re-opening chests will refresh stock.");
				saveShops();
	    		return true;
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
