package net.omnikraft.CleanShop;

import java.util.UUID;

/**
 * This just stores a name and its UUID so we don't have to make constant requests for either.
 *
 */
public class NameToID {
	
	public UUID uuid;
	public String name;
	
	public NameToID(UUID id, String name)
	{
		uuid=id;
		this.name=name;
	}

}
