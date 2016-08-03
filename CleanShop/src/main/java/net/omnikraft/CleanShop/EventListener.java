package net.omnikraft.CleanShop;

import java.util.List;
import java.util.Vector;

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

import net.omnikraft.CleanShop.util.ChestUtils;
import net.omnikraft.CleanShop.util.FileHandler;
import net.omnikraft.CleanShop.util.ShopUtils;

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
    	Vector<ProtectedRegion> regions=ShopUtils.getRegions(b.getLocation());
		
		if(regions!=null)
			for(ProtectedRegion p:regions)
			{
				if(p!=null&&ShopUtils.shopExists(p))
				{
					BlockFace side=getDoubleChestSide(b);
					
					if(side==null) //Single chest
					{
						ChestUtils.clearSingleChest((Chest)b.getState(),ShopUtils.getShop(p));
					}
					else //uuugggghhhh I have to deal with this freaking double chest
					{
						ChestUtils.dealWithThisFreakingDoubleChest((Chest)b.getState(),ShopUtils.getShop(p),side,explodedBlocks);
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
		if(event.getInventory().getType()==InventoryType.CHEST&&CleanShop.shopScan)
		{
			Vector<ProtectedRegion> regions=null;
			if(event.getInventory().getHolder() instanceof Chest)
				regions=ShopUtils.getRegions(((Chest)event.getInventory().getHolder()).getLocation());
			if(event.getInventory().getHolder() instanceof DoubleChest)
				regions=ShopUtils.getRegions(((DoubleChest)event.getInventory().getHolder()).getLocation());
			if(regions!=null)
				for(ProtectedRegion p:regions)
				{
					if(p!=null)
						if(ShopUtils.shopExists(p))
						{
							//plugin.calculateShopStock(plugin.getShop(p));
							if(event.getInventory().getHolder() instanceof Chest)
								ChestUtils.calculateChestStock((Chest)event.getInventory().getHolder(), ShopUtils.getShop(p));
							else
								ChestUtils.calculateChestStock((DoubleChest)event.getInventory().getHolder(), ShopUtils.getShop(p));
							FileHandler.saveShops();
						}
				}
		}
	}

}
