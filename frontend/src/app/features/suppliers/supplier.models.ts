export interface SupplierRegistration {
  legalName: string;
  tradeName: string;
  gstin: string;
  pan: string;
  contactName: string;
  email: string;
  phone: string;
  addressLine1: string;
  city: string;
  stateRegion: string;
  postalCode: string;
  countryCode: string;
}

export interface Supplier {
  supplierId: string;
  supplierNumber: string;
  legalName: string;
  tradeName: string | null;
  gstin: string | null;
  pan: string | null;
  lifecycleStatus: string;
  contactName: string | null;
  email: string | null;
  phone: string | null;
  createdAt: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}
