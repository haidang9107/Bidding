module org.example.frontend {
	requires javafx.controls;
	requires javafx.fxml;

	requires org.controlsfx.controls;
	requires org.kordamp.bootstrapfx.core;

	opens org.example.frontend to javafx.fxml;
	exports org.example.frontend;
}