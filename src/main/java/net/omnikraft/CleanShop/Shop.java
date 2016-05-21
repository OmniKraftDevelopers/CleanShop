package net.omnikraft.CleanShop;

import java.util.Vector;

import org.bukkit.Location;
import org.bukkit.Material;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Shop {
	
	private ProtectedRegion region;
	private Location location;
	private Vector<Material> stock=new Vector<Material>();
	
	public Vector<Material> getStock()
	{
		return stock;
	}
	
	public boolean hasItem(Material m)
	{
		for(Material i: stock)
		{
			if(i==m)
				return true;
		}
		return false;
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

	public void setStock(Vector<Material> stock) {
		this.stock=stock;
	}

}
