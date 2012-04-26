package nova.library.graphics.novagl;

public class NGLTerrainSelection
{
	public static final int MODE_DRAG_RECTANGLE = 1;
	public static final int MODE_DRAG_LINE = 2;
	public static final int MODE_FREEHAND = 3;	
	
	public int mode;
	private Buffer current;
	private Buffer start;
	private Buffer end;
	private NGLTerrain terrain;
	private boolean selection[][];
	
	public NGLTerrainSelection(int mode,NGLTerrain terrain)
	{
		this.mode = mode;
		this.terrain = terrain;
		selection = new boolean[terrain.landcover.length][terrain.landcover[0].length];
		current = new Buffer();
		start = new Buffer();
		end = new Buffer();
	}
	public void clear()
	{
		clearBuffer();
		for(int z = 0; z < selection.length; z++)
		{
			for(int x = 0; x < selection[0].length; x++)
			{
				selection[z][x] = false;
			}
		}
	}
	public void clearBuffer()
	{
		current.clear();
		start.clear();
		end.clear();
	}
	public void calculate()
	{
		if(start.d!=-1&&end.d!=-1)
		{
			//Clear Selection Grid
			for(int z = 0; z < selection.length; z++)
			{
				for(int x = 0; x < selection[0].length; x++)
				{
					selection[z][x] = false;
				}
			}
			
			if(mode==MODE_DRAG_RECTANGLE)
			{
				if(start.z==end.z)
				{
					if(start.x==end.x)
					{
						selection[start.z][start.x] = true;
					}
					else if(start.x<end.x)
					{
						for(int x = start.x; x <= end.x; x++)
						{
							selection[start.z][x] = true;
						}
					}
					else
					{
						for(int x = start.x; x >= end.x; x--)
						{
							selection[start.z][x] = true;
						}
					}
				}
				else if(start.z<end.z)
				{
					if(start.x==end.x)
					{
						for(int z = start.z; z <= end.z; z++)
						{
							selection[z][start.x] = true;
						}
						
					}
					else if(start.x<end.x)
					{
						for(int z = start.z; z <= end.z; z++)
						{
							for(int x = start.x; x <= end.x; x++)
							{
								selection[z][x] = true;
							}
						}
					}
					else
					{
						for(int z = start.z; z <= end.z; z++)
						{
							for(int x = start.x; x >= end.x; x--)
							{
								selection[z][x] = true;
							}
						}
					}
				}
				else if(start.z>end.z)
				{
					if(start.x==end.x)
					{
						for(int z = start.z; z >= end.z; z--)
						{
							selection[z][start.x] = true;
						}
						
					}
					else if(start.x<end.x)
					{
						for(int z = start.z; z >= end.z; z--)
						{
							for(int x = start.x; x <= end.x; x++)
							{
								selection[z][x] = true;
							}
						}
					}
					else
					{
						for(int z = start.z; z >= end.z; z--)
						{
							for(int x = start.x; x >= end.x; x--)
							{
								selection[z][x] = true;
							}
						}
					}
				}
			}
			else if(mode==MODE_DRAG_LINE)
			{
				if(start.z==end.z&&start.x==end.x)
				{
					selection[start.z][start.x] = true;
				}
				else if(Math.abs(end.z-start.z)>=Math.abs(end.x-start.x))
				{
					if(start.z<end.z)
					{
						for(int z = start.z; z <= end.z; z++)
						{
							selection[z][start.x] = true;
						}
					}
					else
					{
						for(int z = start.z; z >= end.z; z--)
						{
							selection[z][start.x] = true;
						}
					}
				}
				else
				{
					if(start.x<end.x)
					{
						for(int x = start.x; x <= end.x; x++)
						{
							selection[start.z][x] = true;
						}
					}
					else
					{
						for(int x = start.x; x >= end.x; x--)
						{
							selection[start.z][x] = true;
						}
					}
				}
			}
		}
	}
	public boolean isCurrent(int x, int z)
	{
		return current.x==x&&current.z==z;
	}
	public boolean isSelected(int x, int z)
	{
		return selection[z][x];
	}
	
	public void start()
	{
		System.err.println("Start Selection");
		start.d = current.d;
		start.x = current.x;
		start.z = current.z;
	}
	public void end()
	{
		System.err.println("End Selection");
		end.d = current.d;
		end.x = current.x;
		end.z = current.z;
	}
	public void setCurrent(double d, int x, int z)
	{
		current.d = d;
		current.x = x;
		current.z = z;
	}
		
	private class Buffer
	{
		//Buffer 
		public double d;
		public int x;
		public int z;
		public Buffer()
		{
			clear();
		}
		public void clear()
		{
			this.d = -1;
			this.x = -1;
			this.z = -1;
		}
	}
}
