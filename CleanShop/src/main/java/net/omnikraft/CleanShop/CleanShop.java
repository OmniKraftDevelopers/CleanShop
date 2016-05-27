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
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
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
	 					System.out.println(c);
	 					JSONObject chest=new JSONObject();
	 					chest.put("x", c.x);
	 					chest.put("y", c.y);
	 					chest.put("z", c.z);
		 				JSONArray mats=new JSONArray();
		 				for(Material m:c.getItems())
		 					mats.add(m.name());
		 				chest.put("items", mats);
		 				chestObjs.add(chest);
	 				}
	 				shop.put("chests", chestObjs);
	 				shopObjs.add(shop);
	 			}
 				obj.put("shops", shopObjs);
	 			
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
	 		 JSONArray shops = (JSONArray)obj.get("shops");
	 		 for(int i=0;i<shops.size();i++)
	 		 {
	 			 JSONObject shop=(JSONObject)shops.get(i);
		         World w=Bukkit.getWorld("world");
		         System.out.println("-------- "+(String)shop.get("name"));
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
			    	 JSONObject chest=(JSONObject)chests.get(i);
			    	 ChestData c=new ChestData((int)chest.get("x"),(int)chest.get("y"),(int)chest.get("z"),null);
			    	 JSONArray mats=(JSONArray)chest.get("items");
			    	 Material[] materials=new Material[mats.size()];
			    	 for(int k=0;k<mats.size();k++)
			    	 {
			    		 materials[i]=Material.getMaterial((String)mats.get(i));
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
	    public void calculateChestStock(Chest c,Shop s)
	    {
	    	ChestData data=s.getChestAt(c.getX(), c.getY(), c.getZ());
	    	if(data==null)
	    	{
	    		data=new ChestData(c.getX(), c.getY(), c.getZ(),null);
	    		s.chestData.add(data);
	    	}

	         invContents(c.getBlockInventory(),data);
	         System.out.println("Single chest");
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
	         System.out.println("Double chest");
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
	               		 System.out.println("Added "+i.getType());
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
	    
	    public void calculateShopStock(Shop s)
	    {
	    	Vector<Chest> queuedChests =new Vector<Chest>();
	    	Vector<Chest> checkedChests =new Vector<Chest>();
	    	ProtectedRegion region=s.getRegion();
	    	 int xMin = region.getMinimumPoint().getBlockX();
	  
	         int xMax = region.getMaximumPoint().getBlockX();
	         int yMax = region.getMaximumPoint().getBlockY();
	         int zMax = region.getMaximumPoint().getBlockZ();
	         
	         Vector<Material> stock = new Vector<Material>();
	         //Iterate through every block in the shop's region
	         for(;xMin<=xMax;xMin++)
	         {
		         int yMin = region.getMinimumPoint().getBlockY();
	             for(;yMin<=yMax;yMin++)
	             {
	    	         int zMin = region.getMinimumPoint().getBlockZ();
	                 for(;zMin<=zMax;zMin++)
	                 {
	                     Block loc= (Block) new Location(s.getLocation().getWorld(), xMin, yMin, zMin).getBlock();
	                    // System.out.println("("+xMin+", "+yMin+", "+zMin+"): "+loc.getType());
	                     
	                     if(loc.getType()==Material.CHEST)
	                     {
	                    	 Chest sc = (Chest) loc.getState();
	                    	 queuedChests.add(sc);
	                     }
	                 }
	             }
	         }
             //Make sure chests don't have a "this is stock"-type sign on it
	         for(Chest c:queuedChests)
	         {
	        	 if(!checkedChests.contains(c))
	        	 {
		        	 boolean isDouble=c.getInventory().getSize()==54;
		        	 
		        	 if(!isStockChest(c))
	            	 {
	                     for(ItemStack i:c.getBlockInventory().getContents())
	                     {
	                    	 if(i!=null&&i.getType()!=null)
	                    	 {
	                    		 //Add every item that isn't named or a diamond.
	                        	 if(!stock.contains(i.getType())&&
	                        			 i.getItemMeta().getDisplayName()==null&&
	                        			 i.getType()!=Material.DIAMOND)
	                        	 {
	                        		 stock.add(i.getType());
	                        		 System.out.println("Added "+i.getType());
	                        	 }
	                    	 }
	                     }
	            	 }
		        	 else if(isDouble)
		        	 {
		        		 Chest otherHalf=findOtherDoubleHalf(c);
		        		 if(otherHalf!=null)
		        		 {
				        	 checkedChests.add(otherHalf);
		        		 }
		        		 else
		        			 System.out.println("AAAAAHHHHH CAN'T FIND OTHER HALF OF DOUBLE CHEST");
		        	 }
		        	 checkedChests.add(c);
	        	 }
	         }
	         s.setStock(stock);
	         saveShops();
	    }
	    
	    private Chest findOtherDoubleHalf(Chest c)
	    {
	    	Location[] loc=new Location[]{
	    			c.getLocation().clone().add(1, 0, 0),
	    			c.getLocation().clone().add(-1, 0, 0),
	    			c.getLocation().clone().add(0, 0, 1),
	    			c.getLocation().clone().add(0, 0, -1)
	    	};
	    	for(Location loc1:loc)
	    		if(loc1.getBlock().getType()==Material.CHEST)
	    			return (Chest)loc1.getBlock().getState();
	    	return null;
	    }
	    
	    /**
	     * @param sc - Chest
	     * @return If any signs attached to the chest say things like "stock"/"supplies"
	     */
	    private boolean isStockChest(Chest sc) {
	    	Vector<Sign> signs = new Vector<Sign>();
	    	Location[] loc=new Location[]{
	    			sc.getLocation().clone().add(1, 0, 0),
	    			sc.getLocation().clone().add(-1, 0, 0),
	    			sc.getLocation().clone().add(0, 0, 1),
	    			sc.getLocation().clone().add(0, 0, -1)
	    	};
	    	
	    	for(Location loc1:loc)
		    	if(loc1.getBlock().getState() instanceof Sign&&loc1.getBlock().getType()==Material.WALL_SIGN)
		    	{
		    		signs.add((Sign)loc1.getBlock().getState());
		    	}
	    	for(Sign s:signs)
	    	{
	    		for(String ss:s.getLines())
	    		{
	    			if(ss.toLowerCase().contains("stock")||ss.toLowerCase().contains("supply")||ss.toLowerCase().contains("supplies"))
	    				return true;
	    		}
	    	}
			return false;
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
						sender.sendMessage("Shops that have "+args[0]+": ");
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
			    				else
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
