import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BackofficeComponent } from './backoffice.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { UsersComponent } from './users/users.component';
import { InterviewsComponent } from './interviews/interviews.component';
import { AnalyticsComponent } from './analytics/analytics.component';
import { QuestionsComponent } from './questions/questions.component';
import { SettingsComponent } from './settings/settings.component';
import { SubscriptionsComponent } from './subscriptions/subscriptions.component';

const routes: Routes = [{ path: '', component: BackofficeComponent ,
   children: [
        { path: '',          redirectTo: 'dashboard', pathMatch: 'full' },
        {path:'dashboard',component:DashboardComponent},
        {path:'users',component:UsersComponent},
        {path:'interviews',component:InterviewsComponent},
        {path:'analytics',component:AnalyticsComponent},
        {path:'questions',component:QuestionsComponent},
        {path:'subscriptions',component:SubscriptionsComponent},
        {path:'settings',component:SettingsComponent},

      ] 
}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class BackofficeRoutingModule { }
