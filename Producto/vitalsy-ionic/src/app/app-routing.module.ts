import { NgModule } from '@angular/core';
import { PreloadAllModules, RouterModule, Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () => import('./home/home.module').then( m => m.HomePageModule)
  },
  {
    path: 'history',
    canActivate: [authGuard],
    loadComponent: () => import('./components/history/history.component').then(m => m.HistoryComponent)
  },
  {
    path: 'calculator',
    canActivate: [authGuard],
    loadComponent: () => import('./components/calculator/calculator.component').then(m => m.CalculatorComponent)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./components/profile/profile.component').then(m => m.ProfileComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./components/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'registro',
    canActivate: [authGuard],
    loadComponent: () => import('./components/registro/registro.component').then(m => m.RegistroComponent)
  },
  {
    path: 'chat',
    canActivate: [authGuard],
    loadComponent: () => import('./components/chat/chat.component').then(m => m.ChatComponent)
  },
  {
    path: 'admin-panel',
    canActivate: [adminGuard],
    loadComponent: () => import('./components/admin-panel/admin-panel.component').then(m => m.AdminPanelComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./components/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, { preloadingStrategy: PreloadAllModules })
  ],
  exports: [RouterModule]
})
export class AppRoutingModule { }
