package de.mein.auth.service;

import de.mein.auth.MeinAuthAdmin;
import de.mein.auth.boot.BootLoaderFX;
import de.mein.auth.data.db.Service;
import de.mein.auth.data.db.ServiceJoinServiceType;
import de.mein.auth.data.db.ServiceType;
import de.mein.auth.gui.*;
import de.mein.auth.tools.N;
import de.mein.sql.RWLock;
import de.mein.sql.SqlQueriesException;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Created by xor on 6/25/16.
 */
public class MeinAuthAdminFX implements Initializable, MeinAuthAdmin {

    private MeinAuthService meinAuthService;
    private Stage stage;

    @FXML
    private TextField txtServiceFilter, txtCertificateFilter;
    @FXML
    private Button btnRefresh, btnApply, btnApprovals, btnCreateService, btnRemoveService, btnOthers;
    @FXML
    private Button btnGeneral, btnDiscover;
    @FXML
    private AnchorPane paneContainer;
    private AuthSettingsFX contentController;
    @FXML
    private ListView<ServiceJoinServiceType> serviceList;
    @FXML
    private ContextMenu createServiceMenu;
    private N runner = new N(Throwable::printStackTrace);
    @FXML
    private TitledPane tpServices;
    @FXML
    private Label lblTitle;
    @FXML
    private HBox hBoxButtons;


    @Override
    public void start(MeinAuthService meinAuthService) {
        this.meinAuthService = meinAuthService;
        showContent();
    }

    @Override
    public void onMessageFromService(MeinService meinService, Object msgObject) {
        N.r(() -> {
            Service service = meinAuthService.getDatabaseManager().getServiceByUuid(meinService.getUuid());
            ServiceType type = meinAuthService.getDatabaseManager().getServiceTypeById(service.getTypeId().v());
            BootLoader bootloader = meinAuthService.getMeinBoot().getBootLoader(type.getType().v());
            if (bootloader instanceof BootLoaderFX) {
                BootLoaderFX bootLoaderFX = (BootLoaderFX) bootloader;
                String containingPath = bootLoaderFX.getPopupFXML(meinService, msgObject);
                loadPopup(containingPath).done(popupContentFX -> popupContentFX.init(meinService, msgObject));
            }
        });
    }

    private Promise<PopupContentFX, Void, Void> loadPopup(String containingPath) {
        Deferred<PopupContentFX, Void, Void> deferred = new DeferredObject<>();
        Platform.runLater(() -> {
            N.r(() -> {
                FXMLLoader loader = new FXMLLoader(MeinAuthAdminFX.class.getClassLoader().getResource("de/mein/auth/popup.fxml"));
                Parent root = null;
                root = loader.load();
                Scene scene = new Scene(root);
                Stage stage = new Stage();
                stage.setTitle("MeinAuthAdmin.Popup '" + meinAuthService.getName() + "'");
                stage.setScene(scene);
                stage.show();
                PopupContainerFX popupController = loader.getController();
                popupController.load(containingPath).done(contentFX -> {
                    contentFX.setStage(stage);
                    deferred.resolve(contentFX);
                });
            });
        });
        return deferred;
        /*new JFXPanel();
        Platform.setImplicitExit(false);
        final MeinAuthAdminFX[] meinAuthFX = new MeinAuthAdminFX[1];
        MeinAuthAdminFX m;
        RWLock lock = new RWLock().lockWrite();
        Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(MeinAuthAdminFX.class.getClassLoader().getResource("de/mein/auth/popup.fxml"));
                        HBox root = null;
                        root = loader.load();
                        meinAuthFX[0] = loader.getController();
                        meinAuthFX[0].start(meinAuthService);
                        Scene scene = new Scene(root);
                        Stage stage = new Stage();
                        stage.setTitle("MeinAuthAdmin '" + meinAuthService.getName() + "'");
                        stage.setScene(scene);
                        stage.show();
                        meinAuthFX[0].setStage(stage);
                        meinAuthFX[0].showContent();
                        lock.unlockWrite();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
        lock.lockWrite().unlockWrite();*/
    }


    public MeinAuthAdminFX() {

    }

    @Override
    public void onChanged() {
        System.out.println("MeinAuthAdminFX.onChanged");
        showContent();
    }

    @Override
    public void shutDown() {
        stage.close();
    }

    private void showContent() {
        try {
            List<ServiceJoinServiceType> serviceJoinServiceTypes = meinAuthService.getDatabaseManager().getAllServices();
            serviceList.getItems().clear();
            for (ServiceJoinServiceType serviceJoinServiceType : serviceJoinServiceTypes) {
                IMeinService runningService = meinAuthService.getMeinService(serviceJoinServiceType.getUuid().v());
                if (runningService != null)
                    serviceJoinServiceType.setRunning(true);
                serviceList.getItems().add(serviceJoinServiceType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        btnGeneral.setOnAction(event -> loadSettingsFX("de/mein/auth/general.fxml"));
        btnRefresh.setOnAction(event -> onChanged());
        btnApply.setOnAction(event -> {
            if (contentController != null) {
                contentController.onApplyClicked();
                contentController = null;
                paneContainer.getChildren().clear();
            }
        });
        btnOthers.setOnAction(event -> {
            loadSettingsFX("de/mein/auth/others.fxml");
        });
        btnApprovals.setOnAction(event -> loadSettingsFX("de/mein/auth/approvals.fxml"));
        btnCreateService.setOnAction(event -> {
            createServiceMenu.getItems().clear();
            int offset = -btnCreateService.heightProperty().intValue();
            Set<String> names = meinAuthService.getMeinBoot().getBootloaderMap().keySet();
            for (String name : names) {
                MenuItem menuItem = new MenuItem(name);
                menuItem.setOnAction(e1 -> {
                    System.out.println("MeinAuthAdminFX.initialize.createmenu.clicked");
                    runner.r(() -> onCreateMenuItemClicked(name));
                });
                createServiceMenu.getItems().add(menuItem);
            }
            createServiceMenu.show(btnCreateService, Side.TOP, 0, offset);
        });
        btnRemoveService.setOnAction(event -> runner.r(() -> {
            ServiceJoinServiceType service = serviceList.getSelectionModel().getSelectedItem();
            meinAuthService.unregisterMeinService(service.getServiceId().v());
            meinAuthService.getDatabaseManager().deleteService(service.getServiceId().v());
        }));
        serviceList.setCellFactory(param -> new ServiceListItem());
        serviceList.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, service) ->
                        runner.r(() -> {
                            if (service != null) {
                                BootLoaderFX bootloader = (BootLoaderFX) meinAuthService.getMeinBoot().getBootLoader(service.getType().v());
                                IMeinService meinService = meinAuthService.getMeinService(service.getUuid().v());
                                loadSettingsFX(bootloader.getEditFXML(meinService));
                                ServiceSettingsFX serviceSettingsFX = (ServiceSettingsFX) contentController;
                                serviceSettingsFX.feed(service);
                            }
                        }));
        btnDiscover.setOnAction(event -> loadSettingsFX("de/mein/auth/discover.fxml"));
        tpServices.expandedProperty().addListener((observable, oldValue, newValue) -> showContent());
    }

    private void onCreateMenuItemClicked(String bootLoaderName) throws IllegalAccessException, SqlQueriesException, InstantiationException {
        Class<? extends BootLoader> bootLoaderClass = meinAuthService.getMeinBoot().getBootloaderMap().get(bootLoaderName);
        BootLoader bootLoader = MeinBoot.createBootLoader(meinAuthService, bootLoaderClass);
        if (bootLoader instanceof BootLoaderFX) {
            loadSettingsFX(((BootLoaderFX) bootLoader).getCreateFXML());
        } else {
            System.out.println("MeinAuthAdminFX.onCreateMenuItemClicked.NO.FX.BOOTLOADER");
        }

    }

    private void loadSettingsFX(String resource) {
        N runner = new N(e -> e.printStackTrace());
        runner.r(() -> {
            showBottomButtons();
            FXMLLoader lo = new FXMLLoader(getClass().getClassLoader().getResource(resource));
            Pane pane = lo.load();
            contentController = lo.getController();
            contentController.configureParentGui(this);
            contentController.setMeinAuthService(meinAuthService);
            lblTitle.setText(contentController.getTitle());
            setContentPane(pane);
            System.out.println("MeinAuthAdminFX.loadSettingsFX.loaded");
        });
    }


    private void setContentPane(Pane pane) {
        paneContainer.getChildren().clear();
        paneContainer.getChildren().add(pane);
        AnchorPane.setBottomAnchor(pane, 0.0);
        AnchorPane.setLeftAnchor(pane, 0.0);
        AnchorPane.setRightAnchor(pane, 0.0);
        AnchorPane.setTopAnchor(pane, 0.0);
    }

    @SuppressWarnings("Duplicates")
    public static MeinAuthAdminFX load(MeinAuthService meinAuthService) {
        new JFXPanel();
        Platform.setImplicitExit(false);
        final MeinAuthAdminFX[] meinAuthAdminFXES = new MeinAuthAdminFX[1];
        MeinAuthAdminFX m;
        RWLock lock = new RWLock().lockWrite();
        Platform.runLater(() -> {
                    try {
                        FXMLLoader loader = new FXMLLoader(MeinAuthAdminFX.class.getClassLoader().getResource("de/mein/auth/mainwindow.fxml"));
                        HBox root = null;
                        root = loader.load();
                        meinAuthAdminFXES[0] = loader.getController();
                        meinAuthAdminFXES[0].start(meinAuthService);
                        Scene scene = new Scene(root);
                        Stage stage = new Stage();
                        stage.setTitle("MeinAuthAdmin '" + meinAuthService.getName() + "'");
                        stage.setScene(scene);
                        stage.show();
                        meinAuthAdminFXES[0].setStage(stage);
                        meinAuthAdminFXES[0].showContent();
                        lock.unlockWrite();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
        lock.lockWrite();
        return meinAuthAdminFXES[0];
    }

    public void hideBottomButtons() {
        hBoxButtons.setVisible(false);
        hBoxButtons.setManaged(false);
    }

    private void showBottomButtons() {
        hBoxButtons.setVisible(true);
        hBoxButtons.setManaged(true);
    }
}