package org.microg.gsf.deviceid;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import org.microg.gsf.IDeviceIdManager;

import java.util.Arrays;

public class ManagerService extends Service implements IDeviceIdManager {
    private static final int FIRST_TIME_CONFIG_VERSION = 1;
    private DatabaseHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DatabaseHelper(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return asBinder();
    }

    private boolean firstTimeConfigDone() {
        return Integer.valueOf(dbHelper.getMeta(DatabaseHelper.META_FIRST_TIME_CONFIG, "0")) == FIRST_TIME_CONFIG_VERSION;
    }

    private void runFirstTimeConfig() {
        Intent intent = new Intent(this, FirstTimeConfig.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    public String getSystemAndroidId() throws RemoteException {
        if (!firstTimeConfigDone()) {
            runFirstTimeConfig();
            return null;
        }
        return null;
    }

    @Override
    public String getSystemDeviceInfo(String identifier) throws RemoteException {
        if (!firstTimeConfigDone()) {
            runFirstTimeConfig();
            return null;
        }
        return null;
    }

    @Override
    public Bundle getSystemAllDeviceInfo() throws RemoteException {
        if (!firstTimeConfigDone()) {
            runFirstTimeConfig();
            return null;
        }
        return null;
    }

    @Override
    public String getAppAndroidId(String packageName) throws RemoteException {
        if (!firstTimeConfigDone()) {
            runFirstTimeConfig();
            return null;
        }
        return null;
    }

    @Override
    public String getAppDeviceInfo(String packageName, String identifier) throws RemoteException {
        if (!firstTimeConfigDone()) {
            runFirstTimeConfig();
            return null;
        }
        return null;
    }

    @Override
    public Bundle getAppAllDeviceInfo(String packageName) throws RemoteException {
        if (!firstTimeConfigDone()) {
            runFirstTimeConfig();
            return null;
        }
        return null;
    }

    @Override
    public void resetSystemAndroidId() throws RemoteException {
        if (!firstTimeConfigDone()) {
            runFirstTimeConfig();
            return;
        }
    }

    @Override
    public void destroyAppAndroidId(String packageName) throws RemoteException {
        if (!firstTimeConfigDone()) {
            runFirstTimeConfig();
            return;
        }
    }

    @Override
    public IBinder asBinder() {
        return new IDeviceIdManager.Stub() {
            @Override
            public String getSystemAndroidId() throws RemoteException {
                if (isPrivileged()) {
                    ManagerService.this.getSystemAndroidId();
                }
                return null;
            }

            @Override
            public String getSystemDeviceInfo(String identifier) throws RemoteException {
                if (isPrivileged()) {
                    ManagerService.this.getSystemDeviceInfo(identifier);
                }
                return null;
            }

            @Override
            public Bundle getSystemAllDeviceInfo() throws RemoteException {
                if (isPrivileged()) {
                    ManagerService.this.getSystemAllDeviceInfo();
                }
                return null;
            }

            @Override
            public String getAppAndroidId(String packageName) throws RemoteException {
                if (isPackageOwner(packageName) || isPrivileged()) {
                    ManagerService.this.getAppAndroidId(packageName);
                }
                return null;
            }

            @Override
            public String getAppDeviceInfo(String packageName, String identifier) throws RemoteException {
                if (isPackageOwner(packageName) || isPrivileged()) {
                    ManagerService.this.getAppDeviceInfo(packageName, identifier);
                }
                return null;
            }

            @Override
            public void resetSystemAndroidId() throws RemoteException {
                if (isPrivileged()) {
                    ManagerService.this.resetSystemAndroidId();
                }
            }

            @Override
            public void destroyAppAndroidId(String packageName) throws RemoteException {
                if (isPackageOwner(packageName) || isPrivileged()) {
                    ManagerService.this.destroyAppAndroidId(packageName);
                }
            }

            @Override
            public Bundle getAppAllDeviceInfo(String packageName) throws RemoteException {
                if (isPackageOwner(packageName) || isPrivileged()) {
                    return ManagerService.this.getAppAllDeviceInfo(packageName);
                }
                return null;
            }

            private boolean isPackageOwner(String packageName) {
                return Arrays.asList(getPackageManager().getPackagesForUid(getCallingUid())).contains(packageName);
            }

            private boolean isPrivileged() {
                // TODO: Do security checks here
                return false;
            }
        };
    }
}
