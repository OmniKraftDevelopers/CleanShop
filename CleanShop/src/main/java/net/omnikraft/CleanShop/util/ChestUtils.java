package net.omnikraft.CleanShop.util;

import java.util.List;
import java.util.Vector;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.omnikraft.CleanShop.chest.ChestData;
import net.omnikraft.CleanShop.shop.Shop;

public class ChestUtils {
	
	public static void clearSingleChest(Chest c, Shop s)
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
    public static void dealWithThisFreakingDoubleChest(Chest c, Shop s, BlockFace side,List<Block> blockList)
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
		if(blockList==null||!blockList.contains(c.getWorld().getBlockAt(x, c.getY(), z)))
			calculateChestStock((Chest)c.getWorld().getBlockAt(x, c.getY(), z).getState(),s);
    }
    
    public static void calculateChestStock(Chest c,Shop s)
    {
    	ChestData data=s.getChestAt(c.getX(), c.getY(), c.getZ());
    	if(data==null)
    	{
    		data=new ChestData(c.getX(), c.getY(), c.getZ(),null);
    		s.chestData.add(data);
    	}

         invContents(c.getBlockInventory(),data);
    }
    public static void calculateChestStock(DoubleChest c,Shop s)
    {
    	ChestData data=s.getChestAt((int)c.getX(), (int)c.getY(), (int)c.getZ());
    	if(data==null)
    	{
    		data=new ChestData((int)c.getX(), (int)c.getY(), (int)c.getZ(),null);
    		s.chestData.add(data);
    	}

         invContents(c.getInventory(),data);
    }
    
    public static void invContents(Inventory blockInv,ChestData data)
    {
    	Vector<Material> stock = new Vector<Material>();
    	for(ItemStack i:blockInv.getContents())
        {
           	 if(i!=null&&i.getType()!=null)
           	 {
               	 if(!stock.contains(i.getType())&&
               			 i.getItemMeta().getDisplayName()==null&&
               			 i.getType()!=Material.DIAMOND)
               	 {
               		 stock.add(i.getType());
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

}
