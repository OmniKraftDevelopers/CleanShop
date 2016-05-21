package net.omnikraft.CleanShop;

import java.util.Vector;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Shop {
	
	private ProtectedRegion region;
	private Location location;
	public Vector<ItemStack> items=new Vector<ItemStack>();
	
	@SuppressWarnings("deprecation")
	public void addItem(String s, byte data)
	{
		Material m = Material.matchMaterial(s);
		
		if(m==null){ return;}

		if(!hasItem(m,data))
		{
			MaterialData md = new MaterialData(m, data);
			items.add(md.toItemStack());
		}
	}
	
	public void addItem(String s)
	{
		addItem(s,(byte)0);
	}
	
	public Vector<ItemStack> getItems()
	{
		return items;
	}

	public void removeItem(String s)
	{
		removeItem(s,(byte)0);
	}
	@SuppressWarnings("deprecation")
	public void removeItem(String s,byte data)
	{
		Material m = Material.matchMaterial(s);
		
		if(m==null) return;
		ItemStack si=null;
		{
			for(ItemStack is:items)
			{
				if(is.getType().equals(m)&&is.getData().getData()==data)
				{
					si=is;
					break;
				}
			}
			if(si!=null)
				items.remove(si);
		}
	}
	
	@SuppressWarnings("deprecation")
	public boolean hasItem(Material m, byte data)
	{
		for(ItemStack is:items)
		{
			if(is.getType().equals(m)&&(data==-1||is.getData().getData()==data))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean hasItem(Material m)
	{
		return hasItem(m,(byte)0);
	}
	
	public Shop setLocation(Location loc)
	{
		this.location=loc;
		return this;
	}
	
	public Location getLocation()
	{
		return location;
	}

	public ProtectedRegion getRegion() {
		return region;
	}

	public void setRegion(ProtectedRegion region) {
		this.region = region;
	}

}
