import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Oauth2CallbackComponent } from './auth/oauth2-callback/oauth2-callback.component';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full'
  },
  // Route de retour de Google OAuth (doit matcher exactement l'URL générée par le backend)
  { path: 'oauth2/callback', component: Oauth2CallbackComponent },
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule)
  },
  {
    path: 'backoffice',
    loadChildren: () => import('./backoffice/backoffice.module').then(m => m.BackofficeModule)
  },
  {
    path: 'frontoffice',
    loadChildren: () => import('./frontoffice/frontoffice.module').then(m => m.FrontofficeModule)
  },
  {
    path: '**',
    redirectTo: 'auth/login'   // ← décommenté : évite l'erreur NG04002
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }