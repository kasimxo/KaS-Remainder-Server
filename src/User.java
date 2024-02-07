
public class User {
	
	private String nombreUsuario;
	
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	public User(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}
	
	@Override
	public int hashCode() {
		return nombreUsuario.hashCode();
	}

	
	@Override
	public String toString() {
		return "Usuario="+ nombreUsuario;
	}
	
}
