import { Component, OnInit } from '@angular/core';
import { UserDTO, UserService } from '../../services/user.service';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {

  users: UserDTO[] = [];
  filteredUsers: UserDTO[] = [];
  loading = false;
  errorMsg = '';

  searchTerm = '';
  roleFilter = '';

  // Toast
  toastMsg = '';
  toastType: 'success' | 'danger' = 'success';
  private toastTimer: any;

  // Modal ajout
  showModal = false;
  newUser: UserDTO = { fullName: '', email: '', age: 18, role: 'USER' };

  // Suppression en attente de confirmation
  userToDelete: UserDTO | null = null;

  // Pagination client
  page = 1;
  pageSize = 10;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAll().subscribe({
      next: (data) => {
        this.users = data;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        this.errorMsg = "Erreur lors du chargement des utilisateurs.";
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();
    this.filteredUsers = this.users.filter(u => {
      const matchesSearch = !term ||
        u.fullName?.toLowerCase().includes(term) ||
        u.email?.toLowerCase().includes(term);
      const matchesRole = !this.roleFilter || u.role === this.roleFilter;
      return matchesSearch && matchesRole;
    });
    this.page = 1;
  }

  onSearchChange(value: string): void {
    this.searchTerm = value;
    this.applyFilters();
  }

  onRoleFilterChange(value: string): void {
    this.roleFilter = value;
    this.applyFilters();
  }

  get pagedUsers(): UserDTO[] {
    const start = (this.page - 1) * this.pageSize;
    return this.filteredUsers.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredUsers.length / this.pageSize));
  }

  nextPage(): void {
    if (this.page < this.totalPages) this.page++;
  }

  prevPage(): void {
    if (this.page > 1) this.page--;
  }

  // --- Modal ajout ---
openModal(): void {
  this.newUser = { fullName: '', email: '', age: 18, role: 'CANDIDATE' };
  this.showModal = true;
}

  closeModal(): void {
    this.showModal = false;
  }

  createUser(): void {
    if (!this.newUser.fullName || !this.newUser.email) {
      this.showToast("Nom et email sont obligatoires.", 'danger');
      return;
    }
    this.userService.create(this.newUser).subscribe({
      next: (created) => {
        this.users.unshift(created);
        this.applyFilters();
        this.closeModal();
        this.showToast('Utilisateur créé !', 'success');
      },
      error: (err) => {
        console.error(err);
        this.showToast("Erreur lors de la création.", 'danger');
      }
    });
  }

  // --- Suppression ---
  askDelete(user: UserDTO): void {
    this.userToDelete = user;
  }

  cancelDelete(): void {
    this.userToDelete = null;
  }

  confirmDelete(): void {
    if (!this.userToDelete?.idUser) return;
    const id = this.userToDelete.idUser;
    this.userService.delete(id).subscribe({
      next: () => {
        this.users = this.users.filter(u => u.idUser !== id);
        this.applyFilters();
        this.userToDelete = null;
        this.showToast('Utilisateur supprimé', 'danger');
      },
      error: (err) => {
        console.error(err);
        this.showToast("Erreur lors de la suppression.", 'danger');
        this.userToDelete = null;
      }
    });
  }

  // --- Helpers UI ---
  getInitials(name: string): string {
    if (!name) return '?';
    const parts = name.trim().split(' ');
    return (parts[0]?.[0] || '') + (parts[1]?.[0] || '');
  }

  avatarColor(id?: number): string {
    const colors = ['#7C3AED', '#10B981', '#F59E0B', '#EF4444', '#06B6D4', '#3B82F6'];
    return colors[(id ?? 0) % colors.length];
  }

  showToast(msg: string, type: 'success' | 'danger' = 'success'): void {
    clearTimeout(this.toastTimer);
    this.toastMsg = msg;
    this.toastType = type;
    this.toastTimer = setTimeout(() => this.toastMsg = '', 2500);
  }
}