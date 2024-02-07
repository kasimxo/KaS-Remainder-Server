import java.util.TimerTask;

public class ConnectedUsers extends TimerTask{

	@Override
	public void run() {
		System.out.println("Listando usuarios conectados:");
		
		for(String usuario : ServidorTCP.usuariosConectados) {
			System.out.println("\t-"+usuario);
		}
	}

}
