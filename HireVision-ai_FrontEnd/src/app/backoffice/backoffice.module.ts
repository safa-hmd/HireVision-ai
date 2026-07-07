import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { BackofficeRoutingModule } from './backoffice-routing.module';
import { BackofficeComponent } from './backoffice.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { MenuComponent } from './menu/menu.component';
import { UsersComponent } from './users/users.component';
import { InterviewsComponent } from './interviews/interviews.component';
import { AnalyticsComponent } from './analytics/analytics.component';
import { QuestionsComponent } from './questions/questions.component';
import { SubscriptionsComponent } from './subscriptions/subscriptions.component';
import { SettingsComponent } from './settings/settings.component';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';


@NgModule({
  declarations: [
    BackofficeComponent,
    DashboardComponent,
    MenuComponent,
    UsersComponent,
    InterviewsComponent,
    AnalyticsComponent,
    QuestionsComponent,
    SubscriptionsComponent,
    SettingsComponent
  ],
  imports: [
    CommonModule,
      
       FormsModule,
       RouterModule, 
    BackofficeRoutingModule,

  ]
})
export class BackofficeModule { }
