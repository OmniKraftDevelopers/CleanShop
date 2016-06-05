package net.omnikraft.CleanShop;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class EventListener implements Listener{
	
	private CleanShop plugin;
	
	public EventListener(CleanShop cs)
	{
		plugin=cs;
	}
	
	private boolean isChest(Block b)
	{
		return b.getType()==Material.CHEST||b.getType()==Material.TRAPPED_CHEST;
	}
	
	//Returns the side a double chest is on. Returns null if it's not a double chest.
	private BlockFace getDoubleChestSide(Block block)
	{
		BlockFace[] aside = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
		for(BlockFace bf : aside)
		{
		    if(block.getRelative(bf, 1).getType() == block.getType())
		        return bf;
		}
		return null;
	}
	
	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		Set<ProtectedRegion> regions=plugin.getRegions(event.getBlock().getLocation()).getRegions();
		
		if(regions!=null)
			for(ProtectedRegion p:regions)
			{
				if(p!=null&&plugin.shopExists(p)&&isChest(event.getBlock()))
				{
					BlockFace side=getDoubleChestSide(event.getBlock());
					
					if(side==null) //Single chest
					{
						plugin.clearSingleChest((Chest)event.getBlock().getState(),plugin.getShop(p));
					}
					else //uuugggghhhh I have to deal with this freaking double chest
					{
						plugin.dealWithThisFreakingDoubleChest((Chest)event.getBlock().getState(),plugin.getShop(p),side);
					}
				}
			}
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
