package net.omnikraft.CleanShop;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

import net.md_5.bungee.api.ChatColor;

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
	
	public void handleChestDestruction(Block b,List<Block> explodedBlocks)
    {
    	Set<ProtectedRegion> regions=plugin.getRegions(b.getLocation()).getRegions();
		
		if(regions!=null)
			for(ProtectedRegion p:regions)
			{
				if(p!=null&&plugin.shopExists(p))
				{
					BlockFace side=getDoubleChestSide(b);
					
					if(side==null) //Single chest
					{
						plugin.clearSingleChest((Chest)b.getState(),plugin.getShop(p));
					}
					else //uuugggghhhh I have to deal with this freaking double chest
					{
						plugin.dealWithThisFreakingDoubleChest((Chest)b.getState(),plugin.getShop(p),side,explodedBlocks);
					}
				}
			}
    }
	
	@EventHandler
	public void onBlockBreakEvent(BlockBreakEvent event) {
		Block b=event.getBlock();
		if(isChest(b))
			handleChestDestruction(b, null);
	}
	
	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		for(Block b:event.blockList())
			if(isChest(b))
				handleChestDestruction(b, event.blockList());
	}	
	
	@EventHandler
	public void onBlockExplodeEvent(BlockExplodeEvent event) {
		Block b=event.getBlock();
		if(isChest(b))
			handleChestDestruction(b, null);
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
