package net.omnikraft.CleanShop;

import java.util.UUID;
import java.util.Vector;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import net.omnikraft.CleanShop.shop.Shop;
import net.omnikraft.CleanShop.util.CommandHandler;
import net.omnikraft.CleanShop.util.FileHandler;
import net.omnikraft.CleanShop.util.NameFetcher;
import net.omnikraft.CleanShop.util.NameToID;
 
public final class CleanShop extends JavaPlugin{
		public WorldGuardPlugin worldGuard;
		public static Vector<NameToID> nameToIDs = new Vector<NameToID>();
		int waitToCopyTimer=0;
		public static boolean shopScan=true;
		public static boolean debug;

	 	public void onEnable() {
	 		FileHandler.plugin=this;
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
	 		

			FileHandler.loadShops();
	    }
	 	
	 	public static void log(String s)
	 	{
	 		if(debug)
	 			System.out.println("CLEANSHOP: "+s);
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
	    
	    public static String getPlayer(UUID id)
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
				addNameUUID(id, p);
			else
				return null;
	    	return p;
	    }
	    
	    public static void addNameUUID(UUID id, String name)
	    {
	    	for(NameToID n:nameToIDs)
	    	{
	    		if(n.uuid.equals(id))
	    			return;
	    	}
	    	nameToIDs.add(new NameToID(id,name));
	    }

		public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
			return CommandHandler.onCommand(sender, cmd, label, args,this);
		}
	
}
