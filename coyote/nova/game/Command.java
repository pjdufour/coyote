package nova.library.


public interface Command
{
	public static final int TYPE_BUILTIN = 1;
	public static final int TYPE_PYTHON = 2;
	public Coyote coyote;
	public int type;

	public Command(Coyote coyote,int type)
	{
		this.coyote = coyote;
		this.type = type;
	}
	public void execute();
}
