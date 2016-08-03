package net.omnikraft.CleanShop.chest;

import org.bukkit.Material;

public class ChestData {
	private Material[] items;
	public int x;
	public int y;
	public int z;
	
	public ChestData(int x, int y, int z, Material[] items)
	{
		this.x=x;
		this.y=y;
		this.z=z;
		this.items=items;
	}
	
	public void setItems(Material[] m)
	{
		items=m;
	}
	
	public Material[] getItems()
	{
		return items;
	}

}
