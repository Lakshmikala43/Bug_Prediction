export declare class EventsService {
    private events;
    getAllEvents(): Promise<{
        id: string;
        title: string;
        date: string;
        location: string;
        capacity: number;
        registered: number;
        type: string;
        price: number;
    }[]>;
    createEvent(data: any): Promise<{
        id: string;
        title: any;
        description: any;
        date: any;
        capacity: any;
        price: any;
        location: string;
        registered: number;
        type: string;
    }>;
}