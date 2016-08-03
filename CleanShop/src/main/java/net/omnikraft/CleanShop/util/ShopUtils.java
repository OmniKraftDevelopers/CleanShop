package net.omnikraft.CleanShop.util;

import java.util.Vector;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.omnikraft.CleanShop.CleanShop;
import net.omnikraft.CleanShop.shop.Shop;

public class ShopUtils {
	
	public static Vector<Shop> shops = new Vector<Shop>();
	public static Vector<RegionManager> regionManagers = new Vector<RegionManager>();
    
    public static Vector<ProtectedRegion> getRegions(Location location)
    {
    	Vector<ProtectedRegion> regions = new Vector<ProtectedRegion>();
    	for(RegionManager rm:regionManagers)
    	{
    		regions.addAll(rm.getApplicableRegions(location).getRegions());
    	}
    	return regions;
    }
	
	public static void updateRegionManagers(CleanShop plugin)
 	{
 		CleanShop.log("Attempting to find all worlds...");
 		CleanShop.log("-----------------------------");
 		regionManagers.clear();
 		for(World w:plugin.getServer().getWorlds())
 		{
 			CleanShop.log("Found world "+w.getName());
 			RegionManager r = plugin.worldGuard.getRegionManager(w);
 			if(r!=null)
 			{
 				CleanShop.log("  Found RegionManager for "+w.getName());
 				regionManagers.add(r);
 			}
 			else
 				CleanShop.log("  Couldn't find RegionManager for "+w.getName());
 		}
 		
 		MultiverseCore m = (MultiverseCore) plugin.getServer().getPluginManager().getPlugin("Multiverse-Core");
 		if(m!=null)
 		{
 			CleanShop.log("Multiverse is loaded. Looking through MV worlds...");
 			for(MultiverseWorld wo:m.getMVWorldManager().getMVWorlds())
 			{
 				World w = wo.getCBWorld();
 				CleanShop.log("Found MV world "+w.getName());
 				RegionManager r = plugin.worldGuard.getRegionManager(w);
 				if(r==null)
 					CleanShop.log("  Couldn't locate a RegionManager for MV World "+w.getName());
 				else
 				{
 					CleanShop.log("  Found RegionManager for MV World "+w.getName());
 					if(!regionManagers.contains(r))
 					{
 						regionManagers.add(r);
 						CleanShop.log("  Added RegionManager for "+w.getName()+" successfully");
 					}
 					else
 						CleanShop.log("  The RegionManager for "+w.getName()+" aleady exists, ignoring.");
 				}
 			}
 		}
 		else
 			CleanShop.log("Multiverse couldn't load.");
 		CleanShop.log("Finished searching for worlds.");
 		CleanShop.log("--------------------------------");
 	}
	
    public static void createShop(ProtectedRegion region)
    {
    	Shop shop = new Shop();
    	shop.setRegion(region);
    	shops.add(shop);
    }

    public static boolean shopExists(ProtectedRegion r)
    {
    	for(Shop s:shops)
    		if(s.getRegion().equals(r))
    			return true;
    	return false;
    }
    
    public static void setTeleport(Shop s,Location v)
    {
    	s.setLocation(v);
    }
    public static Shop getShop(ProtectedRegion r)
    {
    	for(Shop s:shops)
    		if(s!=null&&s.getRegion()!=null&&s.getRegion().equals(r))
    			return s;
    	return null;
    }
    
    public static void removeShop(ProtectedRegion r)
    {
    	Shop removed=null;
    	for(Shop s:shops)
    		if(s.getRegion().equals(r))
    			removed=s;
    	if(removed!=null)
    		shops.remove(removed);
    }
    
    public static Vector<Shop> getOwnedShops(Player player)
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
    
	public static ProtectedRegion getRegion(String s)
	{
		for(RegionManager r:regionManagers)
		{
			ProtectedRegion region = r.getRegion(s);
			if(region!=null)
				return region;
		}
		return null;
	}
	
	public static boolean setShopTeleport(Player player,String[] args)
	{

		if(args.length==0)
		{
			Vector<ProtectedRegion> regions=getRegions(player.getLocation());
			if(regions.size()==0)
			{
				player.sendMessage(ChatColor.RED+"You need to be standing in a WorldGuard region to set the teleport! (Or you can use /setshopteleport [shop])");
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
				
				Shop shop=ShopUtils.getShop(region);
				if(shop!=null)
				{
					ShopUtils.setTeleport(shop,player.getLocation());
					player.sendMessage("Set teleport for shop "+shop.getRegion().getId()+" to your location.");
					FileHandler.saveShops();
				}
				else
					player.sendMessage(ChatColor.RED+"Couldn't find that shop!");
				
				return true;
			}
		}
		else if(args.length==1)
		{
			ProtectedRegion region = ShopUtils.getRegion(args[0]);
			if(region==null)
			{
				player.sendMessage(ChatColor.RED+"No regions could be found with name \""+args[0]+"\".");
				return true;
			}
			else
			{
				if(ShopUtils.shopExists(region))
				{
					Shop shop=ShopUtils.getShop(region);
					if(shop!=null)
					{
						ShopUtils.setTeleport(shop,player.getLocation());
						player.sendMessage("Set teleport for shop "+shop.getRegion().getId()+" to your location.");
						FileHandler.saveShops();
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
		if(s.equalsIgnoreCase("piston"))
			return Material.matchMaterial("piston_base");
		if(s.equalsIgnoreCase("sticky_piston"))
			return Material.matchMaterial("piston_sticky_base");
		if(s.equalsIgnoreCase("comparator"))
			return Material.matchMaterial("redstone_comparator");
		if(s.equalsIgnoreCase("repeater")||s.equalsIgnoreCase("redstone_repeater"))
			return Material.matchMaterial("diode");
		
		return m;
	}
    
    public static Vector<Shop> getShopsWithItem(Player player,String s)
    {
		Material m = ShopUtils.matchMaterial(s);
		Vector<Shop> ss=new Vector<Shop>();
		for(Shop sh:shops)
		{
			if(sh.hasItem(m))
				ss.add(sh);
		}
		return ss;
    }

}
