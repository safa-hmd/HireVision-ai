import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Oauth2CallbackComponent } from './auth/oauth2-callback/oauth2-callback.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';

const routes: Routes = [
  // Page d'accueil → welcome (landing page)
  {
    path: '',
    redirectTo: 'frontoffice/welcome',
    pathMatch: 'full'
  },

  // Callback Google OAuth2
  { path: 'oauth2/callback', component: Oauth2CallbackComponent },

  // Module Auth (login, register, forgot-password…)
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule)
  },

  // Module Backoffice — protégé : connexion requise + rôle ADMIN
  {
    path: 'backoffice',
    canActivate: [RoleGuard],
    loadChildren: () => import('./backoffice/backoffice.module').then(m => m.BackofficeModule)
  },

  // Module Frontoffice — page welcome accessible à tous, reste protégé
  {
    path: 'frontoffice',
    loadChildren: () => import('./frontoffice/frontoffice.module').then(m => m.FrontofficeModule)
  },

  // Page 404 pour toute URL inconnue
  {
    path: '**',
    component: NotFoundComponent
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }