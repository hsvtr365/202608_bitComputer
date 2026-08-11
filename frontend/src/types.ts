export type Role = 'EMPLOYEE' | 'ADMIN'
export type EmployeeStatus = 'ACTIVE' | 'TERMINATED'

export interface OrganizationCode {
  code: string
  name: string
}

export interface AuthUser {
  id: number
  employeeNumber: string
  name: string
  email: string
  role: Role
  status: EmployeeStatus
}

export interface Employee extends AuthUser {
  phone: string | null
  dateOfBirth: string
  department: string
  position: string
  hireDate: string
  terminationDate: string | null
  createdAt: string
  updatedAt: string
}

export interface BackgroundCheckItem {
  checkId: string
  status: 'pending' | 'clear' | 'flagged'
  createdAt: string
  completedAt: string | null
}

export interface BackgroundCheckResult extends BackgroundCheckItem {
  employeeId: string
  firstName: string
  lastName: string
  dateOfBirth: string
  criminalRecord?: boolean | null
  educationVerified?: boolean | null
  employmentVerified?: boolean | null
  creditScore?: string | null
}
