package bookkeeping.server.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BookkeepingService extends Remote {
  void method() throws RemoteException;
  double calculateTax(double saleAmount) throws RemoteException;

  void serializeInvoice(Invoice invoice) throws RemoteException;
}
