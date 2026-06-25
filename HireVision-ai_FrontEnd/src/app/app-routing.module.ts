import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full'
  },
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
