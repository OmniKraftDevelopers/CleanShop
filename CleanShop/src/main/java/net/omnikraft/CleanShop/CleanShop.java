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
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
		
		//TODO: make stock signs only invalidate chests if they're actually attached to them
		//TODO: make placing a sign with the stock keywords check if it's on a chest and recount stock if so
		//TODO: double chests are weird if you put the sign on the wrong side

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
	 			for(Shop s:shops)
	 			{
	 				Location loc=s.getLocation();
	 				String loca="";
	 				if(loc!=null)
	 					loca="LOCATION:"+loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ()+
	 					":"+ loc.getYaw()+":"+loc.getPitch()+ ";";
	 				String items="";
	 				if(s.getStock().size()>0)
	 					items="ITEMS:";
	 				for(int i=0;i<s.getStock().size();i++)
	 				{
	 					items+=s.getStock().get(i).name();
	 					if(i==s.getStock().size()-1)
	 						items+=";";
	 					else
	 						items+=":";
	 				}
	 				writer.write(s.getRegion().getId()+";"+loca+items);
	 				((BufferedWriter) writer).newLine();
	 			}
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
	 		try (
	 		    InputStream fis = new FileInputStream(getShopFile());
	 		    InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
	 		    BufferedReader br = new BufferedReader(isr);
	 		) { while ((line = br.readLine()) != null) {
	 		        String[] all = line.split(";");
	 		        String SHOP_NAME=all[0];
	 		        Location LOCATION=null;
	 		        Vector<Material> ITEMS=null;
	 		        //Name, location, items
 		        	World w=Bukkit.getWorld("world");
	 		        for(String s:all)
	 		        {
	 		        	if(!s.equals(all[0]))
	 		        	{
	 		        		if(s.startsWith("LOCATION:"))
		 		        	{
	 		        			String[] args = s.split(":");
	 		        			World world = Bukkit.getWorld(args[1]);
	 		        			w=world;
		 		        		LOCATION=new Location(world,Double.parseDouble(args[2]),Double.parseDouble(args[3]),Double.parseDouble(args[4]),
		 		        				Float.parseFloat(args[5]),Float.parseFloat(args[6]));
		 		        	}
		 		        	else if(s.startsWith("ITEMS:"))
		 		        	{
	 		        			String[] args = s.split(":");
	 		        			ITEMS = new Vector<Material>();
	 		        			for(String a:args)
	 		        			{
	 		        				if(a!=args[0])
	 		        				{
		 		        				ITEMS.add(matchMaterial(a));
	 		        				}
	 		        			}
		 		        	}
	 		        	}
	 		        }
	 		        if(worldGuard.getRegionManager(w)==null) continue;
	 		        ProtectedRegion region=worldGuard.getRegionManager(w).getRegion(SHOP_NAME);
	 		        this.createShop(region);
	 		        Shop s=getShop(region);
	 		        if(LOCATION!=null)
	 		        	s.setLocation(LOCATION);
	 		        if(ITEMS!=null)
		 		        s.setStock(ITEMS);
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
	    
	    public void calculateShopStock(Shop s)
	    {
	    	
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
	                     
	                     //If it's a chest, make sure it doesn't have a "this is stock"-type sign on it
	                     if(loc.getType()==Material.CHEST)
	                     {
	                    	 Chest sc = (Chest) loc.getState();
	                    	 if(!isStockChest(sc))
	                    	 {
		                         for(ItemStack i:sc.getBlockInventory().getContents())
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
	                     }
	                 }
	             }
	         }
	         s.setStock(stock);
	         saveShops();
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
	    			    		saveShops();
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
