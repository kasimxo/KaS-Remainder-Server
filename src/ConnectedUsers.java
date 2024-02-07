import java.util.TimerTask;

public class ConnectedUsers extends TimerTask{

	@Override
	public void run() {
		System.out.println("Listando usuarios conectados:");
		
		ServidorTCP.usuariosConectados.forEach((usuario, conexion) -> 
			System.out.printf("\tNombre=%s Última conexión=%s",usuario,conexion)
		);
	}

}
