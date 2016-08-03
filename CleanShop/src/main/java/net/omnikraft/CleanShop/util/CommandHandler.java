package net.omnikraft.CleanShop.util;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.omnikraft.CleanShop.CleanShop;
import net.omnikraft.CleanShop.shop.Shop;

public class CommandHandler {
	
	public static CleanShop plugin;

		public static boolean onCommand(CommandSender sender, Command cmd, String label, String[] args,CleanShop plugin) {
			ShopUtils.updateRegionManagers(plugin);
	    	try{
	    	if (cmd.getName().equalsIgnoreCase("createshop")) {
	    		if(sender instanceof Player)
	    		{
	    			Player player = (Player)sender;
	    			if(args.length==0)
	    			{
		    			Vector<ProtectedRegion> regions=ShopUtils.getRegions(player.getLocation());
		    			if(regions.size()==0)
		    			{
							sender.sendMessage(ChatColor.RED+"You need to be standing in a WorldGuard region to create a shop!");
							return true;
		    			}
		    			else if(regions.size()>1)
		    			{
							sender.sendMessage("You are standing in multiple regions and I don't know which one to use for the shop! Use /createShop <regionName> instead.");
							sender.sendMessage("These are the regions you're standing in:");
							for(int i=0;i<regions.size();i++)
								sender.sendMessage(ChatColor.BLUE+regions.get(i).getId());
							return true;
		    			}
		    			else
		    			{
		    				ProtectedRegion[] r = new ProtectedRegion[1];
		    				ProtectedRegion region=regions.toArray(r)[0];
		    				if(!ShopUtils.shopExists(region))
	    					{
		    					ShopUtils.createShop(region);
	    						sender.sendMessage("A shop has been added to region: "+ChatColor.BLUE+region.getId());
	    						sender.sendMessage(ChatColor.YELLOW+"This shop's teleport has been set to your location. Use /sst to change it.");
	    						ShopUtils.setShopTeleport(player, args);
	    			    		FileHandler.saveShops();
	    					}
	    					else
	    						sender.sendMessage(ChatColor.RED+"A shop already exists here!");
		    				return true;
		    			}
	    			}
	    			else if(args.length==1)
	    			{
		    			Vector<ProtectedRegion> regions=ShopUtils.getRegions(player.getLocation());
	    				ProtectedRegion region = ShopUtils.getRegion(args[0]);
	    				if(region==null)
	    				{
							sender.sendMessage(ChatColor.RED+"No regions could be found with name \""+args[0]+"\".");
							return true;
	    				}
	    				else
	    				{
	    					if(!ShopUtils.shopExists(region))
	    					{
	    						ShopUtils.createShop(region);
	    						sender.sendMessage("A shop has been added to region: "+ChatColor.GREEN+args[0]);
	    						if(regions.contains(region))
	    						{
		    						sender.sendMessage(ChatColor.YELLOW+"This shop's teleport has been set to your location. Use /sst to change it.");
		    						ShopUtils.setShopTeleport(player, args);
	    						}
	    			    		FileHandler.saveShops();
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
	    		for(Shop s:ShopUtils.shops)
	    		{
	    			Vector<String> owner=new Vector<String>();
	    			Set<UUID> ids = s.getRegion().getMembers().getUniqueIds();
	    			for(UUID id:ids)
						try {
							String p=CleanShop.getPlayer(id);
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
					no+=ChatColor.GREEN+s.getRegion().getId()+ChatColor.WHITE+(noOwners.lastElement()==s?".":", ");
	    		if(noOwners.size()>0)
	    			sender.sendMessage("These shops have no owners: "+no);
				return true;
	    	}else if (cmd.getName().equalsIgnoreCase("removeshop"))
	    	{
	    		if(sender instanceof Player)
	    		{
	    			if(args.length==1)
	    			{
	    				ProtectedRegion region = ShopUtils.getRegion(args[0]);
	    				boolean r=ShopUtils.shopExists(region);
	    				ShopUtils.removeShop(region);
	    				if(r)
	    				{
	    					sender.sendMessage("Shop "+ChatColor.GREEN+args[0]+ChatColor.WHITE+" removed.");
	    					FileHandler.saveShops();
	    				}
	    				else
	    					sender.sendMessage("No shop named "+ChatColor.RED+args[0]+ChatColor.WHITE+" exists.");
	    				return true;
	    			}
	    		}
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("searchshops")) {
	    		if(args.length==1)
	    		{
					if(ShopUtils.matchMaterial(args[0])==null)
					{
						sender.sendMessage(ChatColor.RED+"That's not an item!");
					}
					else
					{
						Vector<Shop> theShops = ShopUtils.getShopsWithItem((Player)sender,args[0]);
						sender.sendMessage("Shops that have "+ChatColor.BLUE+args[0]+ChatColor.WHITE+": ");
						Vector<String> msgs=new Vector<String>();
						for(Shop s:theShops)
						{
							Vector<String> owner=new Vector<String>();
			    			Set<UUID> ids = s.getRegion().getMembers().getUniqueIds();
			    			for(UUID id:ids)
								try {
									String p=CleanShop.getPlayer(id);
									if(p!=null&&!p.equals(""))
									{
										owner.add(p);
									}
								} catch (Exception e1) {
									e1.printStackTrace();
								}
			    			
			    			if(owner.size()==0)
			    			{
								msgs.add("Shop "+ChatColor.GREEN+s.getRegion().getId()+ChatColor.WHITE+", owned by nobody, apparently.");
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
	    			return ShopUtils.setShopTeleport((Player)sender,args);
	    		FileHandler.saveShops();
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("reloadshops")) {
	    		sender.sendMessage(ChatColor.GRAY+"Reloading shops, please wait...");
	    		ShopUtils.shops.clear();
	    			FileHandler.loadedFile=false;
	    		FileHandler.loadShops();
	    		sender.sendMessage("Shops have been reloaded!");
	    		return true;
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("setshopscan"))
	    	{
	    		if(args.length==1)
	    		{
	    			if(args[0].toLowerCase().equals("true"))
	    			{
	    				CleanShop.shopScan=true;
	    				sender.sendMessage(ChatColor.GREEN+"Enabled shop scanning.");
	    			}
	    			else if(args[0].toLowerCase().equals("false"))
	    			{
	    				CleanShop.shopScan=false;
	    				sender.sendMessage(ChatColor.GREEN+"Disabled shop scanning.");
	    			}
	    			else
	    			{
	    				sender.sendMessage(ChatColor.RED+"Argument must be true or false.");
	    				return false;
	    			}
	    			FileHandler.saveShops();
					return true;
	    		}
	    		else if(args.length==0)
	    			sender.sendMessage(ChatColor.GOLD+"Shop scanning is currently "+(CleanShop.shopScan?"enabled.":"disabled."));
	    		return false;
	    	}
	    	else if (cmd.getName().equalsIgnoreCase("debugcleanshop")) {
	    		if(args.length==1)
	    		{
	    			if(args[0].toLowerCase().equals("true"))
	    			{
	    				CleanShop.debug=true;
	    				sender.sendMessage(ChatColor.GREEN+"Enabled debugging.");
	    			}
	    			else if(args[0].toLowerCase().equals("false"))
	    			{
	    				CleanShop.debug=false;
	    				sender.sendMessage(ChatColor.GREEN+"Disabled debugging.");
	    			}
	    			else
	    			{
	    				sender.sendMessage(ChatColor.RED+"Argument must be true or false.");
	    				return false;
	    			}
	    			FileHandler.saveShops();
					return true;
	    		}
	    		else if(args.length==0)
	    			sender.sendMessage(ChatColor.GOLD+"Debugging is currently "+(CleanShop.debug?"enabled.":"disabled."));
	    		return false;
	    	}else if (cmd.getName().equalsIgnoreCase("wipestock"))
	    	{
	    		if(args.length==0)
	    		{
		    		for(Shop s:ShopUtils.shops)
		    			s.chestData.clear();
		    		sender.sendMessage(ChatColor.GREEN+"All shop stocks have been wiped. Re-opening chests will refresh stock.");
		    		FileHandler.saveShops();
		    		return true;
	    		}
	    		else if(args.length==1)
	    		{
	    			for(Shop s:ShopUtils.shops)
	    				if(s.getRegion().getId().toLowerCase().equals(args[0].toLowerCase()))
	    					s.chestData.clear();
		    		sender.sendMessage(ChatColor.GREEN+"The shop stocks of shop "+args[0]+" have been wiped. Re-opening chests will refresh stock.");
		    		FileHandler.saveShops();
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
			    			for(Shop ss:ShopUtils.shops)
			    			{
			    				if(ss.getRegion().getId().equalsIgnoreCase(args[0]))
			    				{
			    					s=ss;
			    					break;
			    				}
			    			}
			    			
		    				if(s==null)
		    				{
		    					for(Shop sh:ShopUtils.shops)
		    					{
		    						for(UUID i:sh.getRegion().getMembers().getUniqueIds())
		    						{
		    							//System.out.println(i);
		    							{
			    							String n=CleanShop.getPlayer(i);
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
	
}
