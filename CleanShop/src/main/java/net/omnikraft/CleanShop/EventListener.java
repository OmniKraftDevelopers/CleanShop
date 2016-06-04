package net.omnikraft.CleanShop;

import java.util.Set;

import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class EventListener implements Listener{
	
	private CleanShop plugin;
	
	public EventListener(CleanShop cs)
	{
		plugin=cs;
	}

	@EventHandler
	public void onInventoryCloseEvent(InventoryCloseEvent event) {
		//long time=System.nanoTime();
		if(event.getInventory().getType()==InventoryType.CHEST&&plugin.shopScan)
		{
			Set<ProtectedRegion> regions=null;
			if(event.getInventory().getHolder() instanceof Chest)
				regions=plugin.getRegions(((Chest)event.getInventory().getHolder()).getLocation()).getRegions();
			if(event.getInventory().getHolder() instanceof DoubleChest)
				regions=plugin.getRegions(((DoubleChest)event.getInventory().getHolder()).getLocation()).getRegions();
			if(regions!=null)
				for(ProtectedRegion p:regions)
				{
					if(p!=null)
						if(plugin.shopExists(p))
						{
							//plugin.calculateShopStock(plugin.getShop(p));
							if(event.getInventory().getHolder() instanceof Chest)
								plugin.calculateChestStock((Chest)event.getInventory().getHolder(), plugin.getShop(p));
							else
								plugin.calculateChestStock((DoubleChest)event.getInventory().getHolder(), plugin.getShop(p));
							plugin.saveShops();
						}
				}
		}
	}

}
