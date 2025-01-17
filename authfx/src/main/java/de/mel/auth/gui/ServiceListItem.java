package de.mel.auth.gui;

import de.mel.auth.data.db.ServiceJoinServiceType;
import de.mel.auth.service.MelAuthService;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by xor on 9/24/16.
 */
public class ServiceListItem extends ListCell<ServiceJoinServiceType> implements Initializable {
    private MelAuthService melAuthService;
    private ServiceJoinServiceType service;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    @Override
    protected void updateItem(ServiceJoinServiceType service, boolean empty) {
        super.updateItem(service, empty);
        if (!empty) {
            this.setText(service.getType().v() + "/" + service.getName().v());
            if (!service.isRunning()) {
                setStyle("-fx-background-color:rgba(0, 0, 0, 0.05)");
            }
        }
    }
}
