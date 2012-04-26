package nova.library.


public interface Command
{
	public static final int TYPE_BUILTIN = 1;
	public static final int TYPE_PYTHON = 2;
	protected Game game;
	public int type;

	public Command(Game game,int type)
	{
		this.game = game;
		this.type = type;
	}
	public void execute();
}
