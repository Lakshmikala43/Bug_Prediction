var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { Injectable } from '@nestjs/common';
let EventsService = class EventsService {
    events = [
        {
            id: '1',
            title: 'Cloud Architecture Summit 2026',
            date: 'Oct 15, 2026',
            location: 'Tech Convention Center, San Francisco',
            capacity: 1000,
            registered: 840,
            type: 'Conference',
            price: 199,
        },
        {
            id: '2',
            title: 'AI Developers Workshop',
            date: 'Nov 02, 2026',
            location: 'Online',
            capacity: 500,
            registered: 400,
            type: 'Workshop',
            price: 99,
        }
    ];
    async getAllEvents() {
        return this.events;
    }
    async createEvent(data) {
        const newEvent = {
            id: Date.now().toString(),
            title: data.title,
            description: data.description,
            date: data.date,
            capacity: data.capacity,
            price: data.price,
            location: 'TBA',
            registered: 0,
            type: 'Event'
        };
        this.events.push(newEvent);
        return newEvent;
    }
};
EventsService = __decorate([
    Injectable()
], EventsService);
export { EventsService };
//# sourceMappingURL=events.service.js.map