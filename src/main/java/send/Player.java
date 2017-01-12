package send;

public class Player {

	private String league;
	private String team;
	private String player;
	
	public Player(String league, String team, String player){
		this.league = league;
		this.team = team;
		this.player = player;
	}
	
	public String getLeague() {
		return league;
	}
	public void setLeague(String league) {
		this.league = league;
	}
	public String getTeam() {
		return team;
	}
	public void setTeam(String team) {
		this.team = team;
	}
	public String getPlayer() {
		return player;
	}
	public void setPlayer(String player) {
		this.player = player;
	}
	
	
}
